/******************************************************************************
 * @file
 * @brief USART/UART Asynchronous mode Application Note software example
 * @author Energy Micro AS
 * @version 1.02
 ******************************************************************************
 * @section License
 * <b>(C) Copyright 2013 Energy Micro AS, http://www.energymicro.com</b>
 *******************************************************************************
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 * 4. The source and compiled code may only be used on Energy Micro "EFM32"
 *    microcontrollers and "EFR4" radios.
 *
 * DISCLAIMER OF WARRANTY/LIMITATION OF REMEDIES: Energy Micro AS has no
 * obligation to support this Software. Energy Micro AS is providing the
 * Software "AS IS", with no express or implied warranties of any kind,
 * including, but not limited to, any implied warranties of merchantability
 * or fitness for any particular purpose or warranties against infringement
 * of any proprietary rights of a third party.
 *
 * Energy Micro AS will not be liable for any consequential, incidental, or
 * special damages, or any other relief, or for any claim by any third party,
 * arising from your use of this Software.
 *
 *****************************************************************************/

/*******************************
 * THIS IS A MODIFIED VERSION  *
 *******************************/

#include <stdint.h>
#include "em_device.h"
#include "em_chip.h"
#include "em_emu.h"
#include "em_cmu.h"
#include "em_gpio.h"
#include "em_usart.h"
#include "bsp.h"


/* Function prototypes */
void uartSetup(void);
void cmuSetup(void);
void uartPutData(uint8_t uart_channel, uint8_t * dataPtr, uint32_t dataLen);
uint32_t uartGetData(uint8_t * dataPtr, uint32_t dataLen);
void uartPutChar(uint8_t uart_channel, uint8_t ch);
uint8_t uartGetChar(void);


void (*recv_callback)(char);

/* Declare some strings */
const char     welcomeString[]  = "Energy Micro RS-232 - Please press a key\n";
const char     overflowString[] = "\n---RX OVERFLOW---\n";
const uint32_t welLen           = sizeof(welcomeString) - 1;
const uint32_t ofsLen           = sizeof(overflowString) - 1;

/* Define termination character */
#define TERMINATION_CHAR    '.'

/* Declare a circular buffer structure to use for Rx and Tx queues */
#define BUFFERSIZE          256

volatile struct circularBuffer
{
  uint8_t  data[BUFFERSIZE];  /* data buffer */
  uint32_t rdI;               /* read index */
  uint32_t wrI;               /* write index */
  uint32_t pendingBytes;      /* count of how many bytes are not yet handled */
  bool     overflow;          /* buffer overflow indicator */
} rxBuf, txBuf = { {0}, 0, 0, 0, false };


/* Setup UART1 in async mode for RS232*/
static USART_TypeDef           * uart0   = UART0;
static USART_TypeDef           * uart1   = UART1;
static USART_InitAsync_TypeDef uartInit = USART_INITASYNC_DEFAULT;

void setup_uart(void) {
  /* Initialize clocks and oscillators */
  cmuSetup( );

  /* Initialize UART peripheral */
  uartSetup( );

  /* Initialize Development Kit in EBI mode */
  /* BSP_Init(BSP_INIT_DEFAULT); */

  /* Enable RS-232 transceiver on Development Kit */
  /* BSP_PeripheralAccess(/\*BSP_RS232_UART*\/0, true); */
  /* BSP_PeripheralAccess(/\*BSP_RS232_UART*\/1, true); */

  /* When DVK is configured, and no more DVK access is needed, the interface can safely be disabled to save current */
  /* BSP_Disable(); */

 /* Return here, as we don't want to loop indefinitely */
 return;

  /*  Eternal while loop
   *  CPU will sleep during Rx and Tx. When a byte is transmitted, an interrupt
   *  wakes the CPU which copies the next byte in the txBuf queue to the
   *  UART TXDATA register.
   *
   *  When the predefined termiation character is received, the all pending
   *  data in rxBuf is copied to txBuf and echoed back on the UART */
  //while (1)
  //{
  //  //Wait in EM1 while UART transmit
  //  EMU_EnterEM1();

  //  // Check if RX buffer has overflowed
  //  if (rxBuf.overflow) {
  //    rxBuf.overflow = false;
  //    uartPutData((uint8_t*) overflowString, ofsLen);
  //  }

  //  // Check if termination character is received
  //  if (rxBuf.data[(rxBuf.wrI - 1) % BUFFERSIZE] == TERMINATION_CHAR) {
  //    // Copy received data to UART transmit queue
  //    uint8_t tmpBuf[BUFFERSIZE];
  //    int     len = uartGetData(tmpBuf, 0);
  //    uartPutData(tmpBuf, len);
  //  }
  //}
}

/******************************************************************************
* @brief  uartSetup function
*
******************************************************************************/
void uartSetup(void) {
  /* Enable clock for GPIO module (required for pin configuration) */
  CMU_ClockEnable(cmuClock_GPIO, true);
  /* Configure GPIO pins (portB with 9 and 10 is also usable) */
  /* GPIO_PinModeSet(gpioPortE, 0, gpioModePushPull, 1);	//TX */
  /* GPIO_PinModeSet(gpioPortE, 1, gpioModeInput, 0);	//RX */
  GPIO_PinModeSet(gpioPortF, 6, gpioModePushPull, 1);	//TX
  GPIO_PinModeSet(gpioPortF, 7, gpioModeInput, 0);	//RX

  GPIO_PinModeSet(gpioPortF, 10, gpioModePushPull, 1);	//TX
  GPIO_PinModeSet(gpioPortF, 11, gpioModeInput, 0);	//RX


  /* Prepare struct for initializing UART in asynchronous mode*/
  uartInit.enable       = usartDisable;   	/* Don't enable UART upon intialization */
  uartInit.refFreq      = 0;              	/* Provide information on reference frequency. When set to 0, the reference frequency is */
  uartInit.baudrate     = 115200;		/* Baud rate */
  uartInit.oversampling = usartOVS16;     	/* Oversampling. Range is 4x, 6x, 8x or 16x */
  uartInit.databits     = usartDatabits8; 	/* Number of data bits. Range is 4 to 10 */
  uartInit.parity       = usartNoParity;  	/* Parity mode */
  uartInit.stopbits     = usartStopbits1; 	/* Number of stop bits. Range is 0 to 2 */
  uartInit.mvdis        = false;          	/* Disable majority voting */
  uartInit.prsRxEnable  = false;          	/* Enable USART Rx via Peripheral Reflex System */
  uartInit.prsRxCh      = usartPrsRxCh0;  	/* Select PRS channel if enabled */

  /* Initialize USART with uartInit struct */
  USART_InitAsync(uart0, &uartInit);
  USART_InitAsync(uart1, &uartInit);

  // Disable TX on 0 and RX on 1 for now to use USART_{T,R}x directly
  // in main loop to avoid interrupt overhead
  /* Prepare UART Rx and Tx interrupts */
  USART_IntClear(uart0, _UART_IF_MASK);
  USART_IntEnable(uart0, UART_IF_RXDATAV);
  NVIC_ClearPendingIRQ(UART0_RX_IRQn);
  NVIC_ClearPendingIRQ(UART0_TX_IRQn);
  /* NVIC_EnableIRQ(UART0_RX_IRQn); */
  NVIC_EnableIRQ(UART0_TX_IRQn);


  USART_IntClear(uart1, _UART_IF_MASK);
  USART_IntEnable(uart1, UART_IF_RXDATAV);
  NVIC_ClearPendingIRQ(UART1_RX_IRQn);
  NVIC_ClearPendingIRQ(UART1_TX_IRQn);
  NVIC_EnableIRQ(UART1_RX_IRQn);
  /* NVIC_EnableIRQ(UART1_TX_IRQn); */

  /* Enable I/O pins at UART1 location #2 */
  // LOC3 = PE2/PE3
  // LOC2 = PB9/PB10
  // LOC1 = PE0/PE1
  /* uart0->ROUTE = UART_ROUTE_RXPEN | UART_ROUTE_TXPEN | UART_ROUTE_LOCATION_LOC1; */
  uart0->ROUTE = UART_ROUTE_RXPEN | UART_ROUTE_TXPEN | UART_ROUTE_LOCATION_LOC0;
  uart1->ROUTE = UART_ROUTE_RXPEN | UART_ROUTE_TXPEN | UART_ROUTE_LOCATION_LOC1;

  /* Enable UART */
  USART_Enable(uart0, usartEnable);
  USART_Enable(uart1, usartEnable);
}

/******************************************************************************
 * @brief  uartGetChar function
 *
 *  Note that if there are no pending characters in the receive buffer, this
 *  function will hang until a character is received.
 *
 *****************************************************************************/
uint8_t uartGetChar() {
  uint8_t ch;

  /* Check if there is a byte that is ready to be fetched. If no byte is ready, wait for incoming data */
  if (rxBuf.pendingBytes < 1) {
    while(rxBuf.pendingBytes < 1);
  }

  /* Copy data from buffer */
  ch        = rxBuf.data[rxBuf.rdI];
  rxBuf.rdI = (rxBuf.rdI + 1) % BUFFERSIZE;

  /* Decrement pending byte counter */
  rxBuf.pendingBytes--;

  return ch;
}

/******************************************************************************
 * @brief  uartPutChar function
 *
 *****************************************************************************/
void uartPutChar(uint8_t uart_channel, uint8_t ch)
{
  /* Check if Tx queue has room for new data */
  if ((txBuf.pendingBytes + 1) > BUFFERSIZE)
  {
    /* Wait until there is room in queue */
    while ((txBuf.pendingBytes + 1) > BUFFERSIZE) ;
  }

  /* Copy ch into txBuffer */
  txBuf.data[txBuf.wrI] = ch;
  txBuf.wrI             = (txBuf.wrI + 1) % BUFFERSIZE;

  /* Increment pending byte counter */
  txBuf.pendingBytes++;

  /* Enable interrupt on USART TX Buffer*/
  if (uart_channel == 0)
	  USART_IntEnable(uart0, UART_IF_TXBL);
  else
	  USART_IntEnable(uart1, UART_IF_TXBL);
}

/******************************************************************************
 * @brief  uartPutData function
 *
 *****************************************************************************/
void uartPutData(uint8_t uart_channel, uint8_t * dataPtr, uint32_t dataLen) {
  uint32_t i = 0;

  /* Check if buffer is large enough for data */
  if (dataLen > BUFFERSIZE) {
    /* Buffer can never fit the requested amount of data */
    return;
  }

  /* Check if buffer has room for new data */
  if ((txBuf.pendingBytes + dataLen) > BUFFERSIZE) {
    /* Wait until room */
    while ((txBuf.pendingBytes + dataLen) > BUFFERSIZE) ;
  }

  /* Fill dataPtr[0:dataLen-1] into txBuffer */
  while (i < dataLen)
  {
    txBuf.data[txBuf.wrI] = *(dataPtr + i);
    txBuf.wrI             = (txBuf.wrI + 1) % BUFFERSIZE;
    i++;
  }

  /* Increment pending byte counter */
  txBuf.pendingBytes += dataLen;

  /* Enable interrupt on USART TX Buffer*/
  /* USART_IntEnable(uart0, UART_IF_TXBL); */
  if (uart_channel == 0)
      USART_IntEnable(uart0, UART_IF_TXBL);
  else
      USART_IntEnable(uart1, UART_IF_TXBL);
}

/******************************************************************************
 * @brief  uartGetData function
 *
 *****************************************************************************/
uint32_t uartGetData(uint8_t * dataPtr, uint32_t dataLen) {
  uint32_t i = 0;

  /* Wait until the requested number of bytes are available */
  if (rxBuf.pendingBytes < dataLen) {
    while (rxBuf.pendingBytes < dataLen) ;
  }

  if (dataLen == 0) {
    dataLen = rxBuf.pendingBytes;
  }

  /* Copy data from Rx buffer to dataPtr */
  while (i < dataLen) {
    *(dataPtr + i) = rxBuf.data[rxBuf.rdI];
    rxBuf.rdI      = (rxBuf.rdI + 1) % BUFFERSIZE;
    i++;
  }

  /* Decrement pending byte counter */
  rxBuf.pendingBytes -= dataLen;

  return i;
}

/***************************************************************************//**
 * @brief Set up Clock Management Unit
 ******************************************************************************/
void cmuSetup(void) {
  /* Start HFXO and wait until it is stable */
  /* CMU_OscillatorEnable( cmuOsc_HFXO, true, true); */

  /* Select HFXO as clock source for HFCLK */
  /* CMU_ClockSelectSet(cmuClock_HF, cmuSelect_HFXO ); */

  /* Disable HFRCO */
  /* CMU_OscillatorEnable( cmuOsc_HFRCO, false, false ); */

  /* Enable clock for HF peripherals */
  CMU_ClockEnable(cmuClock_HFPER, true);

  /* Enable clock for USART module */
  CMU_ClockEnable(cmuClock_UART0, true);
  CMU_ClockEnable(cmuClock_UART1, true);
}

void UART0_RX_IRQHandler(void) {
  /* Check for RX data valid interrupt */
  if (uart0->STATUS & UART_STATUS_RXDATAV) {
    /* Copy data into RX Buffer */
    uint8_t rxData = USART_Rx(uart0);
    GPIO_PinModeSet(gpioPortE, 1, gpioModePushPull, 1);
    rxBuf.data[rxBuf.wrI] = rxData;
    rxBuf.wrI             = (rxBuf.wrI + 1) % BUFFERSIZE;
    rxBuf.pendingBytes++;

    //Flag Rx overflow
    if (rxBuf.pendingBytes > BUFFERSIZE) {
      rxBuf.overflow = true;
    }

    recv_callback(rxData);

    /* Clear RXDATAV interrupt */
    USART_IntClear(UART0, UART_IF_RXDATAV);
  }
}

void UART0_TX_IRQHandler(void) {
  /* Clear interrupt flags by reading them. */
  USART_IntGet(UART0);

  /* Check TX buffer level status */
  if (uart0->STATUS & UART_STATUS_TXBL)
  {
	GPIO_PinModeSet(gpioPortE, 0, gpioModePushPull, 1);
    if (txBuf.pendingBytes > 0)
    {
      /* Transmit pending character */
      USART_Tx(uart0, txBuf.data[txBuf.rdI]);
      txBuf.rdI = (txBuf.rdI + 1) % BUFFERSIZE;
      txBuf.pendingBytes--;

        //while(txBuf.pendingBytes){
        //    USART_Tx(uart0, txBuf.data[txBuf.rdI]);
        //    txBuf.rdI = (txBuf.rdI + 1) % BUFFERSIZE;
        //    txBuf.pendingBytes--;
        //}
    }

    /* Disable Tx interrupt if no more bytes in queue */
    if (txBuf.pendingBytes == 0)
    {
      USART_IntDisable(uart0, UART_IF_TXBL);
    }
  }
}

void UART1_RX_IRQHandler(void) {
  /* Check for RX data valid interrupt */
  if (uart1->STATUS & UART_STATUS_RXDATAV) {
    /* Copy data into RX Buffer */
    uint8_t rxData = USART_Rx(uart1);
    GPIO_PinModeSet(gpioPortE, 3, gpioModePushPull, 1);
    rxBuf.data[rxBuf.wrI] = rxData;
    rxBuf.wrI             = (rxBuf.wrI + 1) % BUFFERSIZE;
    rxBuf.pendingBytes++;

    //Flag Rx overflow
    if (rxBuf.pendingBytes > BUFFERSIZE) {
      rxBuf.overflow = true;
      //SegmentLCD_Write("overflow");
    }

    //recv_callback(rxData);

    /* Clear RXDATAV interrupt */
    USART_IntClear(UART1, UART_IF_RXDATAV);
  }
}

void UART1_TX_IRQHandler(void) {
  /* Clear interrupt flags by reading them. */
  USART_IntGet(UART1);

  /* Check TX buffer level status */
  if (uart1->STATUS & UART_STATUS_TXBL)
  {
	GPIO_PinModeSet(gpioPortE, 2, gpioModePushPull, 1);
    if (txBuf.pendingBytes > 0)
    {
      /* Transmit pending character */
      USART_Tx(uart1, txBuf.data[txBuf.rdI]);
      txBuf.rdI = (txBuf.rdI + 1) % BUFFERSIZE;
      txBuf.pendingBytes--;
    }

    /* Disable Tx interrupt if no more bytes in queue */
    if (txBuf.pendingBytes == 0)
    {
      USART_IntDisable(uart1, UART_IF_TXBL);
    }
  }
}

void test_callback() {(*recv_callback)('a');}
void set_recv_callback(void (*f)(char)) {recv_callback = f;}
