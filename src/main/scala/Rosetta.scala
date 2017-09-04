package rosetta

import Chisel._
import fpgatidbits.PlatformWrapper._
import fpgatidbits.axi._
import fpgatidbits.dma._
import fpgatidbits.ocm._
import fpgatidbits.regfile._
import scala.collection.mutable.LinkedHashMap

// this file contains the infrastructure that Rosetta instantiates to link
// your accelerator against the existing interfaces on the PYNQ. it also
// defines an (extensible) interface specification for accelerators, in order
// to give easy access to particular I/O functions on the PYNQ board.


// interface definition for PYNQ accelerators
// in addition to the memory access ports and signature, provide access to the
// LEDs and buttons on the PYNQ board
class RosettaAcceleratorIF(numMemPorts: Int) extends Bundle {
  // memory ports to access DRAM. you can use components from fpgatidbits.dma to
  // read and write data through these
  val memPort = Vec.fill(numMemPorts) {new GenericMemoryMasterPort(PYNQParams.toMemReqParams())}
  // use the signature field for sanity and version checks. auto-generated each
  // time the accelerator verilog is regenerated.
  val signature = UInt(OUTPUT, PYNQParams.csrDataBits)
  // user LEDs LD3..0
  val led = UInt(OUTPUT, 4)
  // user switches SW1 and SW0
  val sw = UInt(INPUT, 2)
  // user buttons BN3..0
  val btn = UInt(INPUT, 4)
}

// base class for Rosetta accelerators
abstract class RosettaAccelerator() extends Module {
  def io: RosettaAcceleratorIF
  def numMemPorts: Int

  def hexcrc32(s: String): String = {
    import java.util.zip.CRC32
    val crc=new CRC32
    crc.update(s.getBytes)
    crc.getValue.toHexString
  }

  def makeDefaultSignature(): UInt = {
    import java.util.Date
    import java.text.SimpleDateFormat
    val dateFormat = new SimpleDateFormat("yyyyMMdd");
    val date = new Date();
    val dateString = dateFormat.format(date);
    val fullSignature = this.getClass.getSimpleName + "-" + dateString
    val hexSignature = hexcrc32(fullSignature)

    return UInt("h" + hexSignature)
  }

  // drive default values for memory read port i
  def plugMemReadPort(i: Int) {
    io.memPort(i).memRdReq.valid := Bool(false)
    io.memPort(i).memRdReq.bits.driveDefaults()
    io.memPort(i).memRdRsp.ready := Bool(false)
  }
  // drive default values for memory write port i
  def plugMemWritePort(i: Int) {
    io.memPort(i).memWrReq.valid := Bool(false)
    io.memPort(i).memWrReq.bits.driveDefaults()
    io.memPort(i).memWrDat.valid := Bool(false)
    io.memPort(i).memWrDat.bits := UInt(0)
    io.memPort(i).memWrRsp.ready := Bool(false)
  }
  // use the class name as the accel name
  // just set to something else in derived class if needed
  setName(this.getClass.getSimpleName)
}

// the wrapper, which contains the instantiated accelerator, register file,
// and other components that bridge the accelerator to the rest of the PYNQ
class RosettaWrapper(instFxn: () => RosettaAccelerator) extends Module {
  val p = PYNQParams
  val io = new Bundle {
    // AXI slave interface for control-status registers
    val csr = new AXILiteSlaveIF(p.memAddrBits, p.csrDataBits)
    // AXI master interfaces for reading and writing memory
    val mem = Vec.fill (p.numMemPorts) {
      new AXIMasterIF(p.memAddrBits, p.memDataBits, p.memIDBits)
    }
    // user LEDs LD3..0
    val led = UInt(OUTPUT, 4)
    // user switches SW1 and SW0
    val sw = UInt(INPUT, 2)
    // user buttons BN3..0
    val btn = UInt(INPUT, 4)
  }
  setName("PYNQWrapper")
  setModuleName("PYNQWrapper")
  // a list of files that will be needed for compiling drivers for platform
  val baseDriverFiles: Array[String] = Array[String](
    "platform.h", "wrapperregdriver.h"
  )
  val platformDriverFiles = baseDriverFiles ++ Array[String](
    "platform-xlnk.cpp", "xlnkdriver.hpp"
  )

  // instantiate the accelerator
  val regWrapperReset = Reg(init = Bool(false), clock = Driver.implicitClock)
  val accel = Module(instFxn())

  // ==========================================================================
  // wrapper part 1: register file mappings
  // here we will generate a register file with enough entries to cover the I/O
  // of the accelerator, and link the I/O signals with the respective entry
  // of the register file.
  // =========================================================================
  type RegFileMap = LinkedHashMap[String, Array[Int]]
  // permits controlling the accelerator's reset from both the wrapper's reset,
  // and by using a special register file command (see hack further down :)
  accel.reset := reset | regWrapperReset
  // separate out the mem port signals, won't map the to the regfile
  val ownFilter = {x: (String, Bits) => !(x._1.startsWith("memPort"))}

  import scala.collection.immutable.ListMap
  val ownIO = ListMap(accel.io.flatten.filter(ownFilter).toSeq.sortBy(_._1):_*)

  // each I/O is assigned to at least one register index, possibly more if wide
  // round each I/O width to nearest csrWidth multiple, sum, divide by csrWidth
  val wCSR = p.csrDataBits
  def roundMultiple(n: Int, m: Int) = { (n + m-1) / m * m}
  val fxn = {x: (String, Bits) => (roundMultiple(x._2.getWidth(), wCSR))}
  val numRegs = ownIO.map(fxn).reduce({_+_}) / wCSR

  // instantiate the register file
  val regAddrBits = log2Up(numRegs)
  val regFile = Module(new RegFile(numRegs, regAddrBits, wCSR)).io

  // hack: detect writes to register 0 to control accelerator reset
  val rfcmd = regFile.extIF.cmd
  when(rfcmd.valid & rfcmd.bits.write & rfcmd.bits.regID === UInt(0)) {
    regWrapperReset := rfcmd.bits.writeData(0)
  }

  println("Generating register file mappings...")
  // traverse the accel I/Os and connect to the register file
  var regFileMap = new RegFileMap
  var allocReg = 0
  // hand-place the signature register at 0
  regFileMap("signature") = Array(allocReg)
  regFile.regIn(allocReg).valid := Bool(true)
  regFile.regIn(allocReg).bits := ownIO("signature")
  println("Signal signature mapped to single reg " + allocReg.toString)
  allocReg += 1

  for((name, bits) <- ownIO) {
    if(name != "signature") {
      val w = bits.getWidth()
      if(w > wCSR) {
        // signal is wide, maps to several registers
        val numRegsToAlloc = roundMultiple(w, wCSR) / wCSR
        regFileMap(name) = (allocReg until allocReg + numRegsToAlloc).toArray
        // connect the I/O signal to the register file appropriately
        if(bits.dir == INPUT) {
          // concatanate all assigned registers, connect to input
          bits := regFileMap(name).map(regFile.regOut(_)).reduce(Cat(_,_))
          for(i <- 0 until numRegsToAlloc) {
            regFile.regIn(allocReg + i).valid := Bool(false)
          }
        } else if(bits.dir == OUTPUT) {
          for(i <- 0 until numRegsToAlloc) {
            regFile.regIn(allocReg + i).valid := Bool(true)
            regFile.regIn(allocReg + i).bits := bits(i*wCSR+wCSR-1, i*wCSR)
          }
        } else { throw new Exception("Wire in IO: "+name) }

        println("Signal " + name + " mapped to regs " + regFileMap(name).map(_.toString).reduce(_+" "+_))
        allocReg += numRegsToAlloc
      } else {
        // signal is narrow enough, maps to a single register
        regFileMap(name) = Array(allocReg)
        // connect the I/O signal to the register file appropriately
        if(bits.dir == INPUT) {
          // handle Bool input cases,"multi-bit signal to Bool" error
          if(bits.getWidth() == 1) {
            bits := regFile.regOut(allocReg)(0)
          } else { bits := regFile.regOut(allocReg) }
          // disable internal write for this register
          regFile.regIn(allocReg).valid := Bool(false)

        } else if(bits.dir == OUTPUT) {
          // TODO don't always write (change detect?)
          regFile.regIn(allocReg).valid := Bool(true)
          regFile.regIn(allocReg).bits := bits
        } else { throw new Exception("Wire in IO: "+name) }

        println("Signal " + name + " mapped to single reg " + allocReg.toString)
        allocReg += 1
      }
    }
  }

  // ==========================================================================
  // wrapper part 2: bridging to PYNQ signals
  // here we will instantiate various adapters and similar components to make
  // our accelerator be able to talk to the various interfaces on the PYNQ
  // ==========================================================================
  // rename signals to support Vivado interface inference
  io.csr.renameSignals("csr")
  for(i <- 0 until p.numMemPorts) {io.mem(i).renameSignals(s"mem$i")}

  // connections to board I/O
  accel.io.sw := io.sw
  accel.io.btn := io.btn
  io.led := accel.io.led

  // memory port adapters and connections
  for(i <- 0 until accel.numMemPorts) {
    // instantiate AXI request and response adapters for the mem interface
    val mrp = p.toMemReqParams()
    // read requests
    val readReqAdp = Module(new AXIMemReqAdp(mrp)).io
    readReqAdp.genericReqIn <> accel.io.memPort(i).memRdReq
    readReqAdp.axiReqOut <> io.mem(i).readAddr
    // read responses
    val readRspAdp = Module(new AXIReadRspAdp(mrp)).io
    readRspAdp.axiReadRspIn <> io.mem(i).readData
    readRspAdp.genericRspOut <> accel.io.memPort(i).memRdRsp
    // write requests
    val writeReqAdp = Module(new AXIMemReqAdp(mrp)).io
    writeReqAdp.genericReqIn <> accel.io.memPort(i).memWrReq
    writeReqAdp.axiReqOut <> io.mem(i).writeAddr
    // write data
    // add a small write data queue to ensure we can provide both req ready and
    // data ready at the same time (otherwise this is up to the AXI slave)
    val wrDataQ = FPGAQueue(accel.io.memPort(i).memWrDat, 2)
    // TODO handle this with own adapter?
    io.mem(i).writeData.bits.data := wrDataQ.bits
    // TODO fix this: forces all writes bytelanes valid!
    io.mem(i).writeData.bits.strb := ~UInt(0, width=p.memDataBits/8)
    // TODO fix this: write bursts won't work properly!
    io.mem(i).writeData.bits.last := Bool(true)
    io.mem(i).writeData.valid := wrDataQ.valid
    wrDataQ.ready := io.mem(i).writeData.ready
    // write responses
    val writeRspAdp = Module(new AXIWriteRspAdp(mrp)).io
    writeRspAdp.axiWriteRspIn <> io.mem(i).writeResp
    writeRspAdp.genericRspOut <> accel.io.memPort(i).memWrRsp
  }

  // the accelerator may be using fewer memory ports than what the platform
  // exposes; plug the unused ones
  for(i <- accel.numMemPorts until p.numMemPorts) {
    println("Plugging unused memory port " + i.toString)
    io.mem(i).driveDefaults()
  }

  // AXI regfile read/write logic
  // slow and clumsy, but ctrl/status is not supposed to be performance-
  // critical anyway
  io.csr.writeAddr.ready := Bool(false)
  io.csr.writeData.ready := Bool(false)
  io.csr.writeResp.valid := Bool(false)
  io.csr.writeResp.bits := UInt(0)
  io.csr.readAddr.ready := Bool(false)
  io.csr.readData.valid := Bool(false)
  io.csr.readData.bits.data := regFile.extIF.readData.bits
  io.csr.readData.bits.resp := UInt(0)

  regFile.extIF.cmd.valid := Bool(false)
  regFile.extIF.cmd.bits.driveDefaults()

  val sRead :: sReadRsp :: sWrite :: sWriteD :: sWriteRsp :: Nil = Enum(UInt(), 5)
  val regState = Reg(init = UInt(sRead))

  val regModeWrite = Reg(init=Bool(false))
  val regRdReq = Reg(init=Bool(false))
  val regRdAddr = Reg(init=UInt(0, p.memAddrBits))
  val regWrReq = Reg(init=Bool(false))
  val regWrAddr = Reg(init=UInt(0, p.memAddrBits))
  val regWrData = Reg(init=UInt(0, p.csrDataBits))
  // AXI typically uses byte addressing, whereas regFile indices are
  // element indices -- so the AXI addr needs to be divided by #bytes
  // in one element to get the regFile ind
  // Note that this permits reading/writing only the entire width of one
  // register
  val addrDiv = UInt(p.csrDataBits/8)

  when(!regModeWrite) {
    regFile.extIF.cmd.valid := regRdReq
    regFile.extIF.cmd.bits.read := Bool(true)
    regFile.extIF.cmd.bits.regID := regRdAddr / addrDiv
  } .otherwise {
    regFile.extIF.cmd.valid := regWrReq
    regFile.extIF.cmd.bits.write := Bool(true)
    regFile.extIF.cmd.bits.regID := regWrAddr / addrDiv
    regFile.extIF.cmd.bits.writeData := regWrData
  }

  // state machine for bridging register file reads/writes to AXI slave ops
  switch(regState) {
      is(sRead) {
        io.csr.readAddr.ready := Bool(true)

        when(io.csr.readAddr.valid) {
          regRdReq := Bool(true)
          regRdAddr := io.csr.readAddr.bits.addr
          regModeWrite := Bool(false)
          regState := sReadRsp
        }.otherwise {
          regState := sWrite
        }
      }

      is(sReadRsp) {
        io.csr.readData.valid := regFile.extIF.readData.valid
        when (io.csr.readData.ready & regFile.extIF.readData.valid) {
          regState := sWrite
          regRdReq := Bool(false)
        }
      }

      is(sWrite) {
        io.csr.writeAddr.ready := Bool(true)

        when(io.csr.writeAddr.valid) {
          regModeWrite := Bool(true)
          regWrReq := Bool(false) // need to wait until data is here
          regWrAddr := io.csr.writeAddr.bits.addr
          regState := sWriteD
        } .otherwise {
          regState := sRead
        }
      }

      is(sWriteD) {
        io.csr.writeData.ready := Bool(true)
        when(io.csr.writeData.valid) {
          regWrData := io.csr.writeData.bits.data
          regWrReq := Bool(true) // now we can set the request
          regState := sWriteRsp
        }
      }

      is(sWriteRsp) {
        io.csr.writeResp.valid := Bool(true)
        when(io.csr.writeResp.ready) {
          regWrReq := Bool(false)
          regState := sRead
        }
      }
    }

    // ==========================================================================
    // wrapper part 3: register driver generation functions
    // these functions will be called to generate C++ code that accesses the
    // register file that we have instantiated
    // ==========================================================================

    def makeRegReadFxn(regName: String): String = {
      var fxnStr: String = ""
      val regs = regFileMap(regName)
      if(regs.size == 1) {
        // single register read
        fxnStr += "  AccelReg get_" + regName + "()"
        fxnStr += " {return readReg(" + regs(0).toString + ");} "
      } else if(regs.size == 2) {
        // two-register read
        // TODO this uses a hardcoded assumption about wCSR=32
        if(wCSR != 32) throw new Exception("Violating assumption on wCSR=32")
        fxnStr += "  AccelDblReg get_" + regName + "() "
        fxnStr += "{ return (AccelDblReg)readReg("+regs(1).toString+") << 32 "
        fxnStr += "| (AccelDblReg)readReg("+regs(0).toString+"); }"
      } else { throw new Exception("Multi-reg reads not yet implemented") }

      return fxnStr
    }

    def makeRegWriteFxn(regName: String): String = {
      var fxnStr: String = ""
      val regs = regFileMap(regName)
      if(regs.size == 1) {
        // single register write
        fxnStr += "  void set_" + regName + "(AccelReg value)"
        fxnStr += " {writeReg(" + regs(0).toString + ", value);} "
      } else if(regs.size == 2) {
        // two-register write
        // TODO this uses a hardcoded assumption about wCSR=32
        if(wCSR != 32) throw new Exception("Violating assumption on wCSR=32")
        fxnStr += "  void set_" + regName + "(AccelDblReg value)"
        fxnStr += " { writeReg("+regs(0).toString+", (AccelReg)(value >> 32)); "
        fxnStr += "writeReg("+regs(1).toString+", (AccelReg)(value & 0xffffffff)); }"
      } else { throw new Exception("Multi-reg writes not yet implemented") }

      return fxnStr
    }

    def generateRegDriver(targetDir: String) = {
      var driverStr: String = ""
      val driverName: String = accel.name
      var readWriteFxns: String = ""
      for((name, bits) <- ownIO) {
        if(bits.dir == INPUT) {
          readWriteFxns += makeRegWriteFxn(name) + "\n"
        } else if(bits.dir == OUTPUT) {
          readWriteFxns += makeRegReadFxn(name) + "\n"
        }
      }

      def statRegToCPPMapEntry(regName: String): String = {
        val inds = regFileMap(regName).map(_.toString).reduce(_ + ", " + _)
        return s""" {"$regName", {$inds}} """
      }
      val statRegs = ownIO.filter(x => x._2.dir == OUTPUT).map(_._1)
      val statRegMap = statRegs.map(statRegToCPPMapEntry).reduce(_ + ", " + _)

      driverStr += s"""
  #ifndef ${driverName}_H
  #define ${driverName}_H
  #include "wrapperregdriver.h"
  #include <map>
  #include <string>
  #include <vector>

  using namespace std;
  class $driverName {
  public:
    $driverName(WrapperRegDriver * platform) {
      m_platform = platform;
      attach();
    }
    ~$driverName() {
      detach();
    }

    $readWriteFxns

    map<string, vector<unsigned int>> getStatusRegs() {
      map<string, vector<unsigned int>> ret = {$statRegMap};
      return ret;
    }

    AccelReg readStatusReg(string regName) {
      map<string, vector<unsigned int>> statRegMap = getStatusRegs();
      if(statRegMap[regName].size() != 1) throw ">32 bit status regs are not yet supported from readStatusReg";
      return readReg(statRegMap[regName][0]);
    }

  protected:
    WrapperRegDriver * m_platform;
    AccelReg readReg(unsigned int i) {return m_platform->readReg(i);}
    void writeReg(unsigned int i, AccelReg v) {m_platform->writeReg(i,v);}
    void attach() {m_platform->attach("$driverName");}
    void detach() {m_platform->detach();}
  };
  #endif
      """

      import java.io._
      val writer = new PrintWriter(new File(targetDir+"/"+driverName+".hpp" ))
      writer.write(driverStr)
      writer.close()
      println("=======> Driver written to "+driverName+".hpp")
    }
}
