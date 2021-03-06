EESchema Schematic File Version 4
LIBS:pcb-cache
EELAYER 26 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 10 10
Title ""
Date ""
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L Device:LED D?
U 1 1 5BB6E88B
P 6850 3900
AR Path="/5BA41DB8/5BB6E88B" Ref="D?"  Part="1" 
AR Path="/5BB20D15/5BB6E88B" Ref="D?"  Part="1" 
AR Path="/5BB6E330/5BB6E88B" Ref="D3"  Part="1" 
F 0 "D3" V 6888 3783 50  0000 R CNN
F 1 "LED" V 6797 3783 50  0000 R CNN
F 2 "LED_SMD:LED_1206_3216Metric_Pad1.42x1.75mm_HandSolder" H 6850 3900 50  0001 C CNN
F 3 "~" H 6850 3900 50  0001 C CNN
	1    6850 3900
	0    -1   -1   0   
$EndComp
$Comp
L Device:LED D?
U 1 1 5BB6E892
P 7250 3900
AR Path="/5BA41DB8/5BB6E892" Ref="D?"  Part="1" 
AR Path="/5BB20D15/5BB6E892" Ref="D?"  Part="1" 
AR Path="/5BB6E330/5BB6E892" Ref="D4"  Part="1" 
F 0 "D4" V 7288 3783 50  0000 R CNN
F 1 "LED" V 7197 3783 50  0000 R CNN
F 2 "LED_SMD:LED_1206_3216Metric_Pad1.42x1.75mm_HandSolder" H 7250 3900 50  0001 C CNN
F 3 "~" H 7250 3900 50  0001 C CNN
	1    7250 3900
	0    -1   -1   0   
$EndComp
$Comp
L Device:R R?
U 1 1 5BB6E899
P 6850 3450
AR Path="/5BA41DB8/5BB6E899" Ref="R?"  Part="1" 
AR Path="/5BB20D15/5BB6E899" Ref="R?"  Part="1" 
AR Path="/5BB6E330/5BB6E899" Ref="R34"  Part="1" 
F 0 "R34" H 6920 3496 50  0000 L CNN
F 1 "R" H 6920 3405 50  0000 L CNN
F 2 "Resistor_SMD:R_1206_3216Metric_Pad1.42x1.75mm_HandSolder" V 6780 3450 50  0001 C CNN
F 3 "~" H 6850 3450 50  0001 C CNN
	1    6850 3450
	1    0    0    -1  
$EndComp
$Comp
L Device:R R?
U 1 1 5BB6E8A0
P 7250 3450
AR Path="/5BA41DB8/5BB6E8A0" Ref="R?"  Part="1" 
AR Path="/5BB20D15/5BB6E8A0" Ref="R?"  Part="1" 
AR Path="/5BB6E330/5BB6E8A0" Ref="R35"  Part="1" 
F 0 "R35" H 7320 3496 50  0000 L CNN
F 1 "R" H 7320 3405 50  0000 L CNN
F 2 "Resistor_SMD:R_1206_3216Metric_Pad1.42x1.75mm_HandSolder" V 7180 3450 50  0001 C CNN
F 3 "~" H 7250 3450 50  0001 C CNN
	1    7250 3450
	1    0    0    -1  
$EndComp
Wire Wire Line
	6850 3750 6850 3600
Wire Wire Line
	7250 3600 7250 3750
Text HLabel 6850 3100 1    50   Input ~ 0
LED_1
Text HLabel 7250 3100 1    50   Input ~ 0
LED_2
Wire Wire Line
	7250 3100 7250 3300
Wire Wire Line
	6850 3100 6850 3300
$Comp
L power:GND #PWR?
U 1 1 5BB6E8AD
P 6850 4200
AR Path="/5BA41DB8/5BB6E8AD" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8AD" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8AD" Ref="#PWR075"  Part="1" 
F 0 "#PWR075" H 6850 3950 50  0001 C CNN
F 1 "GND" H 6855 4027 50  0000 C CNN
F 2 "" H 6850 4200 50  0001 C CNN
F 3 "" H 6850 4200 50  0001 C CNN
	1    6850 4200
	1    0    0    -1  
$EndComp
$Comp
L power:GND #PWR?
U 1 1 5BB6E8B3
P 7250 4200
AR Path="/5BA41DB8/5BB6E8B3" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8B3" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8B3" Ref="#PWR076"  Part="1" 
F 0 "#PWR076" H 7250 3950 50  0001 C CNN
F 1 "GND" H 7255 4027 50  0000 C CNN
F 2 "" H 7250 4200 50  0001 C CNN
F 3 "" H 7250 4200 50  0001 C CNN
	1    7250 4200
	1    0    0    -1  
$EndComp
Wire Wire Line
	7250 4050 7250 4200
Wire Wire Line
	6850 4050 6850 4200
$Comp
L freetronics_schematic:SW_PUSHBUTTON SW?
U 1 1 5BB6E8BB
P 4750 3600
AR Path="/5BA41DB8/5BB6E8BB" Ref="SW?"  Part="1" 
AR Path="/5BB20D15/5BB6E8BB" Ref="SW?"  Part="1" 
AR Path="/5BB6E330/5BB6E8BB" Ref="SW12"  Part="1" 
F 0 "SW12" H 4750 3865 50  0000 C CNN
F 1 "SW_PUSHBUTTON" H 4750 3774 50  0000 C CNN
F 2 "myfootprint:SW_PUSHBUTTON_SMD" H 4750 3600 60  0001 C CNN
F 3 "" H 4750 3600 60  0000 C CNN
	1    4750 3600
	1    0    0    -1  
$EndComp
$Comp
L Device:R R?
U 1 1 5BB6E8C2
P 4200 3300
AR Path="/5BA41DB8/5BB6E8C2" Ref="R?"  Part="1" 
AR Path="/5BB20D15/5BB6E8C2" Ref="R?"  Part="1" 
AR Path="/5BB6E330/5BB6E8C2" Ref="R32"  Part="1" 
F 0 "R32" H 4270 3346 50  0000 L CNN
F 1 "R" H 4270 3255 50  0000 L CNN
F 2 "Resistor_SMD:R_1206_3216Metric_Pad1.42x1.75mm_HandSolder" V 4130 3300 50  0001 C CNN
F 3 "~" H 4200 3300 50  0001 C CNN
	1    4200 3300
	1    0    0    -1  
$EndComp
$Comp
L power:GND #PWR?
U 1 1 5BB6E8C9
P 5250 3600
AR Path="/5BA41DB8/5BB6E8C9" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8C9" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8C9" Ref="#PWR073"  Part="1" 
F 0 "#PWR073" H 5250 3350 50  0001 C CNN
F 1 "GND" V 5255 3472 50  0000 R CNN
F 2 "" H 5250 3600 50  0001 C CNN
F 3 "" H 5250 3600 50  0001 C CNN
	1    5250 3600
	0    -1   -1   0   
$EndComp
Wire Wire Line
	4200 3450 4200 3600
Wire Wire Line
	4200 3600 4450 3600
Wire Wire Line
	5050 3600 5250 3600
$Comp
L power:+3.3V #PWR?
U 1 1 5BB6E8D2
P 4200 3000
AR Path="/5BA41DB8/5BB6E8D2" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8D2" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8D2" Ref="#PWR071"  Part="1" 
F 0 "#PWR071" H 4200 2850 50  0001 C CNN
F 1 "+3.3V" H 4215 3173 50  0000 C CNN
F 2 "" H 4200 3000 50  0001 C CNN
F 3 "" H 4200 3000 50  0001 C CNN
	1    4200 3000
	1    0    0    -1  
$EndComp
Wire Wire Line
	4200 3000 4200 3150
Text HLabel 3950 3600 0    50   Input ~ 0
SW_1
Wire Wire Line
	3950 3600 4200 3600
Connection ~ 4200 3600
$Comp
L freetronics_schematic:SW_PUSHBUTTON SW?
U 1 1 5BB6E8DC
P 4750 4550
AR Path="/5BA41DB8/5BB6E8DC" Ref="SW?"  Part="1" 
AR Path="/5BB20D15/5BB6E8DC" Ref="SW?"  Part="1" 
AR Path="/5BB6E330/5BB6E8DC" Ref="SW13"  Part="1" 
F 0 "SW13" H 4750 4815 50  0000 C CNN
F 1 "SW_PUSHBUTTON" H 4750 4724 50  0000 C CNN
F 2 "myfootprint:SW_PUSHBUTTON_SMD" H 4750 4550 60  0001 C CNN
F 3 "" H 4750 4550 60  0000 C CNN
	1    4750 4550
	1    0    0    -1  
$EndComp
$Comp
L Device:R R?
U 1 1 5BB6E8E3
P 4200 4250
AR Path="/5BA41DB8/5BB6E8E3" Ref="R?"  Part="1" 
AR Path="/5BB20D15/5BB6E8E3" Ref="R?"  Part="1" 
AR Path="/5BB6E330/5BB6E8E3" Ref="R33"  Part="1" 
F 0 "R33" H 4270 4296 50  0000 L CNN
F 1 "R" H 4270 4205 50  0000 L CNN
F 2 "Resistor_SMD:R_1206_3216Metric_Pad1.42x1.75mm_HandSolder" V 4130 4250 50  0001 C CNN
F 3 "~" H 4200 4250 50  0001 C CNN
	1    4200 4250
	1    0    0    -1  
$EndComp
$Comp
L power:GND #PWR?
U 1 1 5BB6E8EA
P 5250 4550
AR Path="/5BA41DB8/5BB6E8EA" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8EA" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8EA" Ref="#PWR074"  Part="1" 
F 0 "#PWR074" H 5250 4300 50  0001 C CNN
F 1 "GND" V 5255 4422 50  0000 R CNN
F 2 "" H 5250 4550 50  0001 C CNN
F 3 "" H 5250 4550 50  0001 C CNN
	1    5250 4550
	0    -1   -1   0   
$EndComp
Wire Wire Line
	4200 4400 4200 4550
Wire Wire Line
	4200 4550 4450 4550
Wire Wire Line
	5050 4550 5250 4550
$Comp
L power:+3.3V #PWR?
U 1 1 5BB6E8F3
P 4200 3950
AR Path="/5BA41DB8/5BB6E8F3" Ref="#PWR?"  Part="1" 
AR Path="/5BB20D15/5BB6E8F3" Ref="#PWR?"  Part="1" 
AR Path="/5BB6E330/5BB6E8F3" Ref="#PWR072"  Part="1" 
F 0 "#PWR072" H 4200 3800 50  0001 C CNN
F 1 "+3.3V" H 4215 4123 50  0000 C CNN
F 2 "" H 4200 3950 50  0001 C CNN
F 3 "" H 4200 3950 50  0001 C CNN
	1    4200 3950
	1    0    0    -1  
$EndComp
Wire Wire Line
	4200 3950 4200 4100
Text HLabel 3950 4550 0    50   Input ~ 0
SW_2
Wire Wire Line
	3950 4550 4200 4550
Connection ~ 4200 4550
$EndSCHEMATC
