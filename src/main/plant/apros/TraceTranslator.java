package main.plant.apros;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

public class TraceTranslator {
	final static String INPUT_DIRECTORY = "evaluation/plant-synthesis/vver-traces-plant";

	// scaled x100
	final static Parameter pressurizerWaterLevel = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "water_level", Pair.of(0.0, 850.0), 230.0, 280.0);
	// scaled x10
	final static Parameter pressureInLowerPlenum = new RealParameter(
			"YC00J005#TA11_PRESSURE", "pressure_lower_plenum", Pair.of(0.0, 202.0), 8.0, 35.0, 80.0, 100.0);
	// scaled x100
	final static Parameter liveSteamPressure = new RealParameter(
			"RA00J010#PO11_PRESSURE", "pressure_live_steam", Pair.of(0.0, 510.0), 350.0);
	final static Parameter busbarVoltage = new RealParameter(
			"BU_N1#ES_NO_VOLTAGE_REAL", "voltage", Pair.of(0.0, 7000.0), 4800.0);
	final static Parameter pumpTQ11SpeedSetopint = new RealParameter(
			"TQ11D001_R01#DC2_OUTPUT_VALUE", "tq11_speed_setpoint", Pair.of(0.0, 100.0), 50.0);
	final static Parameter pumpTJ11SpeedSetopint = new RealParameter(
			"TJ11D001_R01#DC2_OUTPUT_VALUE", "tj11_speed_setpoint", Pair.of(0.0, 100.0), 50.0);
	final static Parameter pumpTH11SpeedSetpoint = new RealParameter(
			"TH11D001_R01#DC2_OUTPUT_VALUE", "th11_speed_setpoint", Pair.of(0.0, 100.0), 50.0);
	
	final static Configuration CONF_PROTECTION1 = new Configuration(
			1.0, Arrays.asList(
			pressurizerWaterLevel, pressureInLowerPlenum,
			liveSteamPressure, busbarVoltage), Arrays.asList(
			pumpTQ11SpeedSetopint, pumpTJ11SpeedSetopint,
			pumpTH11SpeedSetpoint));
	
	// scaled x100
	final static Parameter steamGeneratorLevel56 = new RealParameter(
			"YB56W001#SG12_LIQ_LEVEL", "level56x", Pair.of(0.0, 300.0), 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel54 = new RealParameter(
			"YB54W001#SG12_LIQ_LEVEL", "level54x", Pair.of(0.0, 300.0), 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel52 = new RealParameter(
			"YB52W001#SG12_LIQ_LEVEL", "level52x", Pair.of(0.0, 300.0), 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel15 = new RealParameter(
			"YB15W001#SG12_LIQ_LEVEL", "level15x", Pair.of(0.0, 300.0), 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel13 = new RealParameter(
			"YB13W001#SG12_LIQ_LEVEL", "level13x", Pair.of(0.0, 300.0), 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel11 = new RealParameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x", Pair.of(0.0, 300.0), 196.0);
	final static Parameter prot7pumpSpeed = new RealParameter(
			"RL92D001_PU1#P_SPEED_OLD", "prot7_pump_speed", Pair.of(0.0, 100.0), 50.0);
	final static Parameter prot7ValveOpen = new BoolParameter(
			"RL92S005_VA1#VO_OPEN", "prot7_valve_open");
	final static Parameter prot7ValveClose = new BoolParameter(
			"RL92S005_VA1#VO_CLOSE", "prot7_valve_close");
	final static Parameter prot7toProt5signal64 = new BoolParameter(
			"YZU001XL64#BINARY_VALUE", "prot7_signal64_");
	final static Parameter prot7toProt5signal65 = new BoolParameter(
			"YZU001XL65#BINARY_VALUE", "prot7_signal65_");
	
	final static Parameter prot7ValveOpened = new BoolParameter(
			"RL92S005_VA1#VO_VALVE_OPEN", "prot7_valve_opened");
	final static Parameter prot7ValveClosed = new BoolParameter(
			"RL92S005_VA1#VO_VALVE_CLOSE", "prot7_valve_closed");
	
	final static Configuration CONF_PROTECTION7 = new Configuration(
			1.0,
			Arrays.asList(
					steamGeneratorLevel56, steamGeneratorLevel54,
					steamGeneratorLevel52, steamGeneratorLevel15,
					steamGeneratorLevel13, steamGeneratorLevel11,
					busbarVoltage,
					prot7ValveOpened, prot7ValveClosed),
			Arrays.asList(
					prot7pumpSpeed, prot7ValveOpen, prot7ValveClose,
					prot7toProt5signal64, prot7toProt5signal65
				));

	// scaled x10
	final static Parameter steamGeneratorPressure56_prot5 = new RealParameter(
			"YB56W001#SG12_PRESSURE_3_4", "pressure56x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	// scaled x10
	final static Parameter steamGeneratorPressure54_prot5 = new RealParameter(
			"YB54W001#SG12_PRESSURE_3_4", "pressure54x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	// scaled x10
	final static Parameter steamGeneratorPressure52_prot5 = new RealParameter(
			"YB52W001#SG12_PRESSURE_3_4", "pressure52x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	// scaled x10
	final static Parameter steamGeneratorPressure15_prot5 = new RealParameter(
			"YB15W001#SG12_PRESSURE_3_4", "pressure15x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	// scaled x10
	final static Parameter steamGeneratorPressure13_prot5 = new RealParameter(
			"YB13W001#SG12_PRESSURE_3_4", "pressure13x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	// scaled x10
	final static Parameter steamGeneratorPressure11_prot5 = new RealParameter(
			"YB11W001#SG12_PRESSURE_3_4", "pressure11x", Pair.of(20.0, 60.0), 40.0); // random cutoff
	
	final static Parameter prot5valve41open = new BoolParameter(
			"RL41S001_VA1#VO_OPEN", "valve41open");
	final static Parameter prot5valve41close = new BoolParameter(
			"RL41S001_VA1#VO_CLOSE", "valve41close");
	final static Parameter prot5valve42open = new BoolParameter(
			"RL42S001_VA1#VO_OPEN", "valve42open");
	final static Parameter prot5valve42close = new BoolParameter(
			"RL42S001_VA1#VO_CLOSE", "valve42close");
	final static Parameter prot5valve43open = new BoolParameter(
			"RL43S001_VA1#VO_OPEN", "valve43open");
	final static Parameter prot5valve43close = new BoolParameter(
			"RL43S001_VA1#VO_CLOSE", "valve43close");
	final static Parameter prot5valve44open = new BoolParameter(
			"RL44S001_VA1#VO_OPEN", "valve44open");
	final static Parameter prot5valve44close = new BoolParameter(
			"RL44S001_VA1#VO_CLOSE", "valve44close");
	final static Parameter prot5valve45open = new BoolParameter(
			"RL45S001_VA1#VO_OPEN", "valve45open");
	final static Parameter prot5valve45close = new BoolParameter(
			"RL45S001_VA1#VO_CLOSE", "valve45close");
	final static Parameter prot5valve46open = new BoolParameter(
			"RL46S001_VA1#VO_OPEN", "valve46open");
	final static Parameter prot5valve46close = new BoolParameter(
			"RL46S001_VA1#VO_CLOSE", "valve46close");
	
	final static Parameter prot5valve41valveopen = new BoolParameter(
			"RL41S001_VA1#VO_VALVE_OPEN", "valve41valveopen");
	final static Parameter prot5valve41valveclose = new BoolParameter(
			"RL41S001_VA1#VO_VALVE_CLOSE", "valve41valveclose");
	final static Parameter prot5valve42valveopen = new BoolParameter(
			"RL42S001_VA1#VO_VALVE_OPEN", "valve42valveopen");
	final static Parameter prot5valve42valveclose = new BoolParameter(
			"RL42S001_VA1#VO_VALVE_CLOSE", "valve42valveclose");
	final static Parameter prot5valve43valveopen = new BoolParameter(
			"RL43S001_VA1#VO_VALVE_OPEN", "valve43valveopen");
	final static Parameter prot5valve43valveclose = new BoolParameter(
			"RL43S001_VA1#VO_VALVE_CLOSE", "valve43valveclose");
	final static Parameter prot5valve44valveopen = new BoolParameter(
			"RL44S001_VA1#VO_VALVE_OPEN", "valve44valveopen");
	final static Parameter prot5valve44valveclose = new BoolParameter(
			"RL44S001_VA1#VO_VALVE_CLOSE", "valve44valveclose");
	final static Parameter prot5valve45valveopen = new BoolParameter(
			"RL45S001_VA1#VO_VALVE_OPEN", "valve45valveopen");
	final static Parameter prot5valve45valveclose = new BoolParameter(
			"RL45S001_VA1#VO_VALVE_CLOSE", "valve45valveclose");
	final static Parameter prot5valve46valveopen = new BoolParameter(
			"RL46S001_VA1#VO_VALVE_OPEN", "valve46valveopen");
	final static Parameter prot5valve46valveclose = new BoolParameter(
			"RL46S001_VA1#VO_VALVE_CLOSE", "valve46valveclose");
	
	final static Configuration CONF_PROTECTION5 = new Configuration(
			1.0, Arrays.asList(
			steamGeneratorPressure56_prot5,
			steamGeneratorPressure54_prot5,
			steamGeneratorPressure52_prot5,
			steamGeneratorPressure15_prot5,
			steamGeneratorPressure13_prot5,
			steamGeneratorPressure11_prot5,
			prot7toProt5signal64,
			prot7toProt5signal65,
			prot5valve41valveopen, prot5valve41valveclose,
			prot5valve42valveopen, prot5valve42valveclose,
			prot5valve43valveopen, prot5valve43valveclose,
			prot5valve44valveopen, prot5valve44valveclose,
			prot5valve45valveopen, prot5valve45valveclose,
			prot5valve46valveopen, prot5valve46valveclose
			), Arrays.asList(
			prot5valve41open, prot5valve41close,
			prot5valve42open, prot5valve42close,
			prot5valve43open, prot5valve43close,
			prot5valve44open, prot5valve44close,
			prot5valve45open, prot5valve45close,
			prot5valve46open, prot5valve46close));

	// scaled x100
	final static Parameter pressurizerWaterLevel_entirePlant = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "pressurizer_water_level", Pair.of(0.0, 850.0), 230.0, 280.0,
			370.5);
	// scaled x10
	final static Parameter pressureInLowerPlenum_entirePlant = new RealParameter(
			"YC00J005#TA11_PRESSURE", "pressure_lower_plenum", Pair.of(0.0, 202.0), 35.0, 80.0, 100.0);
	// scaled x100
	final static Parameter liveSteamPressure_entirePlant = new RealParameter(
			"RA00J010#PO11_PRESSURE", "pressure_live_steam", Pair.of(0.0, 510.0), 300.0, 350.0);
	final static Parameter busbarVoltage_entirePlant = new RealParameter(
			"BU_N1#ES_NO_VOLTAGE_REAL", "voltage", Pair.of(0.0, 7000.0), 4800.0);
	// scaled x100
	final static Parameter steamGeneratorLevel56_entirePlant = new RealParameter(
			"YB56W001#SG12_LIQ_LEVEL", "level56x", Pair.of(0.0, 300.0), 180.0, 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel54_entirePlant = new RealParameter(
			"YB54W001#SG12_LIQ_LEVEL", "level54x", Pair.of(0.0, 300.0), 180.0, 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel52_entirePlant = new RealParameter(
			"YB52W001#SG12_LIQ_LEVEL", "level52x", Pair.of(0.0, 300.0), 180.0, 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel15_entirePlant = new RealParameter(
			"YB15W001#SG12_LIQ_LEVEL", "level15x", Pair.of(0.0, 300.0), 180.0, 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel13_entirePlant = new RealParameter(
			"YB13W001#SG12_LIQ_LEVEL", "level13x", Pair.of(0.0, 300.0), 180.0, 196.0);
	// scaled x100
	final static Parameter steamGeneratorLevel11_entirePlant = new RealParameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x", Pair.of(0.0, 3.0), 1.8, 1.96);
	// scaled x10
	final static Parameter steamGeneratorPressure56_entirePlant = new RealParameter(
			"YB56W001#SG12_PRESSURE_3_4", "pressure56x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter steamGeneratorPressure54_entirePlant = new RealParameter(
			"YB54W001#SG12_PRESSURE_3_4", "pressure54x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter steamGeneratorPressure52_entirePlant = new RealParameter(
			"YB52W001#SG12_PRESSURE_3_4", "pressure52x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter steamGeneratorPressure15_entirePlant = new RealParameter(
			"YB15W001#SG12_PRESSURE_3_4", "pressure15x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter steamGeneratorPressure13_entirePlant = new RealParameter(
			"YB13W001#SG12_PRESSURE_3_4", "pressure13x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter steamGeneratorPressure11_entirePlant = new RealParameter(
			"YB11W001#SG12_PRESSURE_3_4", "pressure11x", Pair.of(0.0, 60.0));
	// scaled x10
	final static Parameter reacRelPower_entirePlant = new RealParameter(
			"YC00B001#NR1_POWER", "reac_rel_power", Pair.of(0.0, 13.0), 1.0, 9.0, 10.0, 11.0);
	// scaled x10
	final static Parameter pressureUpperPlenum_entirePlant = new RealParameter(
			"YC00J030#TA11_PRESSURE", "pressure_upper_plenum", Pair.of(0.0, 200.0), 93.0, 108.0, 134.0);
	final static Parameter tempUpperPlenum_entirePlant = new RealParameter(
			"YC00J030#TA11_TEMPERATURE", "temp_upper_plenum", Pair.of(0.0, 500.0), 180.0, 317.0);
	final static Parameter tripSignal = new BoolParameter(
			"YZ10U404FL01#FF_OUTPUT_VALUE", "trip");

	final static Parameter prot6valveA11open = new BoolParameter(
			"RA11S003_VA1#VO_OPEN", "valveA11open");
	final static Parameter prot6valveA11close = new BoolParameter(
			"RA11S003_VA1#VO_CLOSE", "valveA11close");
	final static Parameter prot6valveA52open = new BoolParameter(
			"RA52S003_VA1#VO_OPEN", "valveA52open");
	final static Parameter prot6valveA52close = new BoolParameter(
			"RA52S003_VA1#VO_CLOSE", "valveA52close");
	final static Parameter prot6valveA13open = new BoolParameter(
			"RA13S003_VA1#VO_OPEN", "valveA13open");
	final static Parameter prot6valveA13close = new BoolParameter(
			"RA13S003_VA1#VO_CLOSE", "valveA13close");
	final static Parameter prot6valveA54open = new BoolParameter(
			"RA54S003_VA1#VO_OPEN", "valveA54open");
	final static Parameter prot6valveA54close = new BoolParameter(
			"RA54S003_VA1#VO_CLOSE", "valveA54close");
	final static Parameter prot6valveA15open = new BoolParameter(
			"RA15S003_VA1#VO_OPEN", "valveA15open");
	final static Parameter prot6valveA15close = new BoolParameter(
			"RA15S003_VA1#VO_CLOSE", "valveA15close");
	final static Parameter prot6valveA56open = new BoolParameter(
			"RA56S003_VA1#VO_OPEN", "valveA56open");
	final static Parameter prot6valveA56close = new BoolParameter(
			"RA56S003_VA1#VO_CLOSE", "valveA56close");
	final static Parameter prot6valveL31open = new BoolParameter(
			"RL31S003_VA1#VO_OPEN", "valveL31open");
	final static Parameter prot6valveL31close = new BoolParameter(
			"RL31S003_VA1#VO_CLOSE", "valveL31close");
	final static Parameter prot6valveL72open = new BoolParameter(
			"RL72S003_VA1#VO_OPEN", "valveL72open");
	final static Parameter prot6valveL72close = new BoolParameter(
			"RL72S003_VA1#VO_CLOSE", "valveL72close");
	final static Parameter prot6valveL33open = new BoolParameter(
			"RL33S003_VA1#VO_OPEN", "valveL33open");
	final static Parameter prot6valveL33close = new BoolParameter(
			"RL33S003_VA1#VO_CLOSE", "valveL33close");
	final static Parameter prot6valveL74open = new BoolParameter(
			"RL74S003_VA1#VO_OPEN", "valveL74open");
	final static Parameter prot6valveL74close = new BoolParameter(
			"RL74S003_VA1#VO_CLOSE", "valveL74close");
	final static Parameter prot6valveL35open = new BoolParameter(
			"RL35S003_VA1#VO_OPEN", "valveL35open");
	final static Parameter prot6valveL35close = new BoolParameter(
			"RL35S003_VA1#VO_CLOSE", "valveL35close");
	final static Parameter prot6valveL76open = new BoolParameter(
			"RL76S003_VA1#VO_OPEN", "valveL76open");
	final static Parameter prot6valveL76close = new BoolParameter(
			"RL76S003_VA1#VO_CLOSE", "valveL76close");

	final static Parameter prot6valveA11valveopen = new BoolParameter(
			"RA11S003_VA1#VO_VALVE_OPEN", "valveA11valveopen");
	final static Parameter prot6valveA11valveclose = new BoolParameter(
			"RA11S003_VA1#VO_VALVE_CLOSE", "valveA11valveclose");
	final static Parameter prot6valveA52valveopen = new BoolParameter(
			"RA52S003_VA1#VO_VALVE_OPEN", "valveA52valveopen");
	final static Parameter prot6valveA52valveclose = new BoolParameter(
			"RA52S003_VA1#VO_VALVE_CLOSE", "valveA52valveclose");
	final static Parameter prot6valveA13valveopen = new BoolParameter(
			"RA13S003_VA1#VO_VALVE_OPEN", "valveA13valveopen");
	final static Parameter prot6valveA13valveclose = new BoolParameter(
			"RA13S003_VA1#VO_VALVE_CLOSE", "valveA13valveclose");
	final static Parameter prot6valveA54valveopen = new BoolParameter(
			"RA54S003_VA1#VO_VALVE_OPEN", "valveA54valveopen");
	final static Parameter prot6valveA54valveclose = new BoolParameter(
			"RA54S003_VA1#VO_VALVE_CLOSE", "valveA54valveclose");
	final static Parameter prot6valveA15valveopen = new BoolParameter(
			"RA15S003_VA1#VO_VALVE_OPEN", "valveA15valveopen");
	final static Parameter prot6valveA15valveclose = new BoolParameter(
			"RA15S003_VA1#VO_VALVE_CLOSE", "valveA15valveclose");
	final static Parameter prot6valveA56valveopen = new BoolParameter(
			"RA56S003_VA1#VO_VALVE_OPEN", "valveA56valveopen");
	final static Parameter prot6valveA56valveclose = new BoolParameter(
			"RA56S003_VA1#VO_VALVE_CLOSE", "valveA56valveclose");
	final static Parameter prot6valveL31valveopen = new BoolParameter(
			"RL31S003_VA1#VO_VALVE_OPEN", "valveL31valveopen");
	final static Parameter prot6valveL31valveclose = new BoolParameter(
			"RL31S003_VA1#VO_VALVE_CLOSE", "valveL31valveclose");
	final static Parameter prot6valveL72valveopen = new BoolParameter(
			"RL72S003_VA1#VO_VALVE_OPEN", "valveL72valveopen");
	final static Parameter prot6valveL72valveclose = new BoolParameter(
			"RL72S003_VA1#VO_VALVE_CLOSE", "valveL72valveclose");
	final static Parameter prot6valveL33valveopen = new BoolParameter(
			"RL33S003_VA1#VO_VALVE_OPEN", "valveL33valveopen");
	final static Parameter prot6valveL33valveclose = new BoolParameter(
			"RL33S003_VA1#VO_VALVE_CLOSE", "valveL33valveclose");
	final static Parameter prot6valveL74valveopen = new BoolParameter(
			"RL74S003_VA1#VO_VALVE_OPEN", "valveL74valveopen");
	final static Parameter prot6valveL74valveclose = new BoolParameter(
			"RL74S003_VA1#VO_VALVE_CLOSE", "valveL74valveclose");
	final static Parameter prot6valveL35valveopen = new BoolParameter(
			"RL35S003_VA1#VO_VALVE_OPEN", "valveL35valveopen");
	final static Parameter prot6valveL35valveclose = new BoolParameter(
			"RL35S003_VA1#VO_VALVE_CLOSE", "valveL35valveclose");
	final static Parameter prot6valveL76valveopen = new BoolParameter(
			"RL76S003_VA1#VO_VALVE_OPEN", "valveL76valveopen");
	final static Parameter prot6valveL76valveclose = new BoolParameter(
			"RL76S003_VA1#VO_VALVE_CLOSE", "valveL76valveclose");
	
	final static Configuration CONF_PROTECTION6 = new Configuration(
				1.0, Arrays.asList(
				liveSteamPressure_entirePlant,
				prot6valveA11valveopen, prot6valveA11valveclose,
				prot6valveA52valveopen, prot6valveA52valveclose,
				prot6valveA13valveopen, prot6valveA13valveclose,
				prot6valveA54valveopen, prot6valveA54valveclose,
				prot6valveA15valveopen, prot6valveA15valveclose,
				prot6valveA56valveopen, prot6valveA56valveclose,
				prot6valveL31valveopen, prot6valveL31valveclose,
				prot6valveL72valveopen, prot6valveL72valveclose,
				prot6valveL33valveopen, prot6valveL33valveclose,
				prot6valveL74valveopen, prot6valveL74valveclose,
				prot6valveL35valveopen, prot6valveL35valveclose,
				prot6valveL76valveopen, prot6valveL76valveclose
				), Arrays.asList(
				prot6valveA11open, prot6valveA11close,
				prot6valveA52open, prot6valveA52close,
				prot6valveA13open, prot6valveA13close,
				prot6valveA54open, prot6valveA54close,
				prot6valveA15open, prot6valveA15close,
				prot6valveA56open, prot6valveA56close,
				prot6valveL31open, prot6valveL31close,
				prot6valveL72open, prot6valveL72close,
				prot6valveL33open, prot6valveL33close,
				prot6valveL74open, prot6valveL74close,
				prot6valveL35open, prot6valveL35close,
				prot6valveL76open, prot6valveL76close));

	final static Parameter coolantPumpStopped51 = new BoolParameter(
			"SK00C010XG51#BINARY_VALUE", "coolantPumpStopped51");
	final static Parameter coolantPumpStopped52 = new BoolParameter(
			"SK00C010XG52#BINARY_VALUE", "coolantPumpStopped52");
	final static Parameter coolantPumpStopped53 = new BoolParameter(
			"SK00C010XG53#BINARY_VALUE", "coolantPumpStopped53");
	final static Parameter coolantPumpStopped54 = new BoolParameter(
			"SK00C010XG54#BINARY_VALUE", "coolantPumpStopped54");
	final static Parameter coolantPumpStopped55 = new BoolParameter(
			"SK00C010XG55#BINARY_VALUE", "coolantPumpStopped55");
	final static Parameter coolantPumpStopped56 = new BoolParameter(
			"SK00C010XG56#BINARY_VALUE", "coolantPumpStopped56");
	// scaled x10
	final static Parameter rodPosition = new RealParameter(
			"YC00B001_RA1#RA_RE_RODP2", "rodPosition", Pair.of(0.0, 25.0), 5.0);

	// scaled x100
	final static Parameter steamGeneratorLevel56_reaTurTrip = new RealParameter(
			"YB56W001#SG12_LIQ_LEVEL", "level56x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter steamGeneratorLevel54_reaTurTrip = new RealParameter(
			"YB54W001#SG12_LIQ_LEVEL", "level54x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter steamGeneratorLevel52_reaTurTrip = new RealParameter(
			"YB52W001#SG12_LIQ_LEVEL", "level52x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter steamGeneratorLevel15_reaTurTrip = new RealParameter(
			"YB15W001#SG12_LIQ_LEVEL", "level15x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter steamGeneratorLevel13_reaTurTrip = new RealParameter(
			"YB13W001#SG12_LIQ_LEVEL", "level13x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter steamGeneratorLevel11_reaTurTrip = new RealParameter(
			"YB11W001#SG12_LIQ_LEVEL", "level11x", Pair.of(0.0, 300.0), 180.0);
	// scaled x100
	final static Parameter pressurizerWaterLevel_reaTurTrip = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "pressurizer_water_level", Pair.of(0.0, 850.0), 370.5);
	// scaled x10
	final static Parameter reacRelPower_reaTurTrip = new RealParameter(
			"YC00B001#NR1_POWER", "reac_rel_power", Pair.of(0.0, 13.0), 1.0, 3.0, 5.0, 7.0, 9.0, 11.0);
	// scaled x100
	final static Parameter liveSteamPressure_reaTurTrip = new RealParameter(
			"RA00J010#PO11_PRESSURE", "pressure_live_steam", Pair.of(0.0, 510.0), 300.0);
	
	final static Configuration CONF_REA_TUR_TRIP = new Configuration(
			1.0, Arrays.asList(
				liveSteamPressure_reaTurTrip,
				reacRelPower_reaTurTrip,
				tempUpperPlenum_entirePlant,
				pressureUpperPlenum_entirePlant,
				pressurizerWaterLevel_reaTurTrip,
				steamGeneratorLevel56_reaTurTrip,
				steamGeneratorLevel54_reaTurTrip,
				steamGeneratorLevel52_reaTurTrip,
				steamGeneratorLevel15_reaTurTrip,
				steamGeneratorLevel13_reaTurTrip,
				steamGeneratorLevel11_reaTurTrip,
				coolantPumpStopped51,
				coolantPumpStopped52,
				coolantPumpStopped53,
				coolantPumpStopped54,
				coolantPumpStopped55,
				coolantPumpStopped56
			), Arrays.asList(rodPosition, tripSignal));
	// there is also YZU001XL48#BINARY_VALUE from protection6,
	// but it just states that liveSteamPressure < 3


	final static Configuration CONF_PLANT = new Configuration(
			1.0,
			Arrays.asList(
					pressurizerWaterLevel_entirePlant,
					pressureInLowerPlenum_entirePlant,
					liveSteamPressure_entirePlant,
					//busbarVoltage_entirePlant,
					steamGeneratorLevel56_entirePlant,
					//steamGeneratorLevel54_entirePlant,
					//steamGeneratorLevel52_entirePlant,
					steamGeneratorLevel15_entirePlant,
					//steamGeneratorLevel13_entirePlant,
					//steamGeneratorLevel11_entirePlant,
					//steamGeneratorPressure56_entirePlant,
					//steamGeneratorPressure54_entirePlant,
					//steamGeneratorPressure52_entirePlant,
					//steamGeneratorPressure15_entirePlant,
					//steamGeneratorPressure13_entirePlant,
					//steamGeneratorPressure11_entirePlant,
					reacRelPower_entirePlant,
					pressureUpperPlenum_entirePlant,
					tempUpperPlenum_entirePlant
				), Arrays.asList(tripSignal));
	
	static {
		// some of the trip conditions
		CONF_PLANT.addColorRule(liveSteamPressure_entirePlant, 0, "yellow");
		CONF_PLANT.addColorRule(reacRelPower_entirePlant, 4, "yellow");
		CONF_PLANT.addColorRule(tempUpperPlenum_entirePlant, 2, "yellow");
		CONF_PLANT.addColorRule(pressureUpperPlenum_entirePlant, 2, "yellow");

		// reactor relative power
		CONF_PLANT.addColorRule(reacRelPower_entirePlant, 0, "blue");
		CONF_PLANT.addColorRule(reacRelPower_entirePlant, 3, "red");
		CONF_PLANT.addColorRule(reacRelPower_entirePlant, 4, "red");
	}
	
	final static Parameter binSigFromReaPowLimit_reacco = new BoolParameter(
			"YK00_ROMXL01#BINARY_VALUE", "bin_sig_rea_pow_limit");
	final static Parameter anSigFromReaPowLimit_reacco = new RealParameter(
			"YK00_ROMXJ35#ANALOG_VALUE", "an_sig_rea_pow_limit", Pair.of(0.0, 2.5), 1.0, 2.0);
	final static Parameter rodPosition_reacco = new RealParameter(
			"YC00B001_RA1#RA_RE_RODP", "rod_position", Pair.of(0.0, 2.5), 1.0, 2.0);
	
	final static Configuration CONF_REACTOR_CO = new Configuration(
			1.0,
			Arrays.asList(
					binSigFromReaPowLimit_reacco,
					liveSteamPressure_entirePlant,
					anSigFromReaPowLimit_reacco,
					reacRelPower_entirePlant
			), Arrays.asList(rodPosition_reacco));
	
	final static Parameter YA11T001_preslevco = new RealParameter(
			"YA11T001#ME_OUTPUT_VALUE", "YA11T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA11T002_preslevco = new RealParameter(
			"YA11T002#ME_OUTPUT_VALUE", "YA11T002", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA12T001_preslevco = new RealParameter(
			"YA12T001#ME_OUTPUT_VALUE", "YA12T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA12T002_preslevco = new RealParameter(
			"YA12T002#ME_OUTPUT_VALUE", "YA12T002", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA13T001_preslevco = new RealParameter(
			"YA13T001#ME_OUTPUT_VALUE", "YA13T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA13T002_preslevco = new RealParameter(
			"YA13T002#ME_OUTPUT_VALUE", "YA13T002", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA14T001_preslevco = new RealParameter(
			"YA14T001#ME_OUTPUT_VALUE", "YA14T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA14T002_preslevco = new RealParameter(
			"YA14T002#ME_OUTPUT_VALUE", "YA14T002", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA15T001_preslevco = new RealParameter(
			"YA15T001#ME_OUTPUT_VALUE", "YA15T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA15T002_preslevco = new RealParameter(
			"YA15T002#ME_OUTPUT_VALUE", "YA15T002", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA16T001_preslevco = new RealParameter(
			"YA16T001#ME_OUTPUT_VALUE", "YA16T001", Pair.of(0.0, 350.0), 270.0);
	final static Parameter YA16T002_preslevco = new RealParameter(
			"YA16T002#ME_OUTPUT_VALUE", "YA16T002", Pair.of(0.0, 350.0), 270.0);
	
	final static Parameter valveE51_preslevco = new BoolParameter(
			"TE51S002_VA1#V_POSITION_SET_VALUE", "valveE51");
	final static Parameter valveK52_preslevco = new BoolParameter(
			"TK52S002_VA1#V_POSITION_SET_VALUE", "valveK52");
	final static Parameter valveK53_preslevco = new BoolParameter(
			"TK53S002_VA1#V_POSITION_SET_VALUE", "valveK53");
	
	// scaled x100
	final static Parameter pressurizerWaterLevel_preslevco = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "pressurizer_water_level", Pair.of(0.0, 850.0),
			100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 750.0, 800.0);
	
	final static Configuration CONF_PRES_LEV_CONT = new Configuration(
			1.0,
			Arrays.asList(
				YA11T001_preslevco, YA11T002_preslevco,
				YA12T001_preslevco, YA12T002_preslevco,
				YA13T001_preslevco, YA13T002_preslevco,
				YA14T001_preslevco, YA14T002_preslevco,
				YA15T001_preslevco, YA15T002_preslevco,
				YA16T001_preslevco, YA16T002_preslevco,
				pressurizerWaterLevel_preslevco
			), Arrays.asList(valveE51_preslevco,
					valveK52_preslevco, valveK53_preslevco));
	
	// scaled x100
	final static Parameter pressurizerWaterLevel_prespresco = new RealParameter(
			"YP10B001#PR11_LIQ_LEVEL", "pressurizer_water_level", Pair.of(0.0, 850.0),
			231.0);
	// scaled x 1/10000
	final static Parameter pressurizerPressure_prespresco = new RealParameter(
			"YP10B001_NO8#NO6_PRESSURE", "pressurizer_pressure", Pair.of(0.0, 20e2),
			11e2, 12e2, 1221.0, 1240.0, 1250.0, 1260.0, 1270.0, 1280.0, 1294.0);
	// scaled x100
	final static Parameter power_prespresco = new RealParameter(
			"YP10B001_HS1#HS_POWER", "power",  Pair.of(0.0, 100.0), 50.0);
	final static Parameter valve1311_prespresco = new BoolParameter(
			"YP13S011_VA1#V_POSITION_SET_VALUE", "valve1311");
	final static Parameter valve1411_prespresco = new BoolParameter(
			"YP14S011_VA1#V_POSITION_SET_VALUE", "valve1411");
	final static Parameter valve1308_prespresco = new BoolParameter(
			"YP13S008_VA1#V_POSITION_SET_VALUE", "valve1308");
	final static Parameter valve1408_prespresco = new BoolParameter(
			"YP14S008_VA1#V_POSITION_SET_VALUE", "valve1408");
	final static Parameter valve1305_prespresco = new BoolParameter(
			"YP13S005_VA1#V_POSITION_SET_VALUE", "valve1305");
	final static Parameter valve1405_prespresco = new BoolParameter(
			"YP14S005_VA1#V_POSITION_SET_VALUE", "valve1405");
	final static Parameter valve1302_prespresco = new BoolParameter(
			"YP13S002_VA1#V_POSITION_SET_VALUE", "valve1302");
	final static Parameter valve1402_prespresco = new BoolParameter(
			"YP14S002_VA1#V_POSITION_SET_VALUE", "valve1402");
	
	final static Configuration CONF_PRES_PRES_CONT = new Configuration(
			1.0,
			Arrays.asList(
					pressurizerWaterLevel_prespresco,
					pressurizerPressure_prespresco
			), Arrays.asList(power_prespresco,
					valve1311_prespresco,
					valve1411_prespresco,
					valve1308_prespresco,
					valve1408_prespresco,
					valve1305_prespresco,
					valve1405_prespresco,
					valve1302_prespresco,
					valve1402_prespresco));
	
	// multiplied x1000
	final static Parameter level10 = new RealParameter(
			"RL10B001#TA11_LIQ_LEVEL", "level10",  Pair.of(0.0, 3210.0),
			1900.0, 2000.0, 2100.0, 2200.0, 2300.0, 2400.0, 2500.0, 2600.0, 2700.0);
	// multiplied x1000
	final static Parameter level50 = new RealParameter(
			"RL50B001#TA11_LIQ_LEVEL", "level50",  Pair.of(0.0, 3210.0),
			1900.0, 2000.0, 2100.0, 2200.0, 2300.0, 2400.0, 2500.0, 2600.0, 2700.0);
	
	// multiplied x100
	final static Parameter valveRV10S005 = new RealParameter(
			"RV10S005_R01#DC2_OUTPUT_VALUE", "valveRV10S005",  Pair.of(0.0, 100.0), 50.0);
	// multiplied x100
	final static Parameter valveRV12S004 = new RealParameter(
			"RV12S004_R01#DC2_OUTPUT_VALUE", "valveRV12S004",  Pair.of(0.0, 100.0), 50.0);
	// multiplied x100
	final static Parameter valveRL10S008 = new RealParameter(
			"RL10S008_R01#DC2_OUTPUT_VALUE", "valveRL10S008",  Pair.of(0.0, 100.0), 50.0);
	// multiplied x100
	final static Parameter valveRV50S005 = new RealParameter(
			"RV50S005_R01#DC2_OUTPUT_VALUE", "valveRV50S005",  Pair.of(0.0, 100.0), 50.0);
	// multiplied x100
	final static Parameter valveRV52S004 = new RealParameter(
			"RV52S004_R01#DC2_OUTPUT_VALUE", "valveRV52S004",  Pair.of(0.0, 100.0), 50.0);
	// multiplied x100
	final static Parameter valveRL50S008 = new RealParameter(
			"RL50S008_R01#DC2_OUTPUT_VALUE", "valveRL50S008",  Pair.of(0.0, 100.0), 50.0);
	
	final static Configuration CONF_FW_LEVEL_CO = new Configuration(
			1.0,
			Arrays.asList(
				level10, level50	
			), Arrays.asList(
				valveRV10S005, valveRV12S004, valveRL10S008,
				valveRV50S005, valveRV52S004, valveRL50S008
			));

	// to improve precision in the NuSMV model
	final static Map<String, Double> PARAM_SCALES = new TreeMap<>();
	static {
		PARAM_SCALES.put(pressurizerWaterLevel.aprosName(), 100.0);
		PARAM_SCALES.put(pressureInLowerPlenum.aprosName(), 10.0);
		PARAM_SCALES.put(liveSteamPressure.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorPressure56_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorPressure54_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorPressure52_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorPressure15_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorPressure13_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorPressure11_prot5.aprosName(), 10.0);
		PARAM_SCALES.put(steamGeneratorLevel56.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorLevel54.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorLevel52.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorLevel15.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorLevel13.aprosName(), 100.0);
		PARAM_SCALES.put(steamGeneratorLevel11.aprosName(), 100.0);
		PARAM_SCALES.put(pressureUpperPlenum_entirePlant.aprosName(), 10.0);
		PARAM_SCALES.put(reacRelPower_entirePlant.aprosName(), 10.0);
		PARAM_SCALES.put(rodPosition.aprosName(), 10.0);
		PARAM_SCALES.put(power_prespresco.aprosName(), 100.0);
		PARAM_SCALES.put(pressurizerPressure_prespresco.aprosName(), 1e-4);
		PARAM_SCALES.put(level10.aprosName(), 1000.0);
		PARAM_SCALES.put(level50.aprosName(), 1000.0);
		PARAM_SCALES.put(valveRV10S005.aprosName(), 100.0);
		PARAM_SCALES.put(valveRV12S004.aprosName(), 100.0);
		PARAM_SCALES.put(valveRL10S008.aprosName(), 100.0);
		PARAM_SCALES.put(valveRV50S005.aprosName(), 100.0);
		PARAM_SCALES.put(valveRV52S004.aprosName(), 100.0);
		PARAM_SCALES.put(valveRL50S008.aprosName(), 100.0);
	}
	
	
	private final static Configuration CONFIGURATION = CONF_PROTECTION1;

	private final static String OUTPUT_TRACE_FILENAME = "evaluation/plant-synthesis/vver.sc";
	private final static String OUTPUT_ACTIONSPEC_FILENAME = "evaluation/plant-synthesis/vver.actionspec";
	private final static String OUTPUT_LTL_FILENAME = "evaluation/plant-synthesis/vver.ltl";
	
	private static void allEventCombinations(char[] arr, int index, Set<String> result, List<Parameter> parameters) {
		if (index == arr.length) {
			result.add(String.valueOf(arr));
		} else {
			final int intervalNum = parameters.get(index - 1).valueCount();
			for (int i = 0; i < intervalNum; i++) {
				arr[index] = Character.forDigit(i, 10);
				allEventCombinations(arr, index + 1, result, parameters);
			}
		}
	}
	
	public static List<String> generateScenarios(Configuration conf, Dataset ds, Set<List<String>> allActionCombinations,
			String gvOutput, String smvOutput, boolean addActionDescriptions, int sizeThreshold,
			boolean allEventCombinations) throws FileNotFoundException {
		// traces
		final Set<String> allEvents = new TreeSet<>();
		if (allEventCombinations) {
			// complete event set
			final char[] arr = new char[conf.inputParameters.size() + 1];
			arr[0] = 'A';
			allEventCombinations(arr, 1, allEvents, conf.inputParameters);
		}
		
		// coverage
		final Set<Pair<String, Integer>> inputCovered = new HashSet<>();
		final Set<Pair<String, Integer>> outputCovered = new HashSet<>();
		int totalInputValues = 0;
		int totalOutputValues = 0;
		for (Parameter p : conf.inputParameters) {
			totalInputValues += p.valueCount();
		}
		for (Parameter p : conf.outputParameters) {
			totalOutputValues += p.valueCount();
		}
		
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_TRACE_FILENAME))) {
			for (List<double[]> trace : ds.values) {
				final List<String> events = new ArrayList<>();
				final List<String> actions = new ArrayList<>();

				for (double[] snapshot : trace) {
					final StringBuilder event = new StringBuilder("A");
					final List<String> thisActions = new ArrayList<>();
					
					for (Parameter p : conf.inputParameters) {
						final double value = ds.get(snapshot, p);
						final int index = p.traceNameIndex(value);
						inputCovered.add(Pair.of(p.aprosName(), index));
						event.append(index);
					}
					
					for (Parameter p : conf.outputParameters) {
						final double value = ds.get(snapshot, p);
						final int index = p.traceNameIndex(value);
						outputCovered.add(Pair.of(p.aprosName(), index));
						thisActions.add(p.traceName(value));
					}
					
					events.add(event.toString());
					actions.add(String.join(", ", thisActions));
					allActionCombinations.add(thisActions);
					allEvents.add(event.toString());
				}
				
				events.add(0, "");
				events.remove(events.size() - 1);
				pw.println(String.join("; ", events));
				pw.println(String.join("; ", actions));
			}
		}

		// actionspec
		try (PrintWriter pw = new PrintWriter(new File(
				OUTPUT_ACTIONSPEC_FILENAME))) {
			for (Parameter p : conf.outputParameters) {
				for (String str : p.actionspec()) {
					pw.println(str);
				}
			}
		}

		// temporal properties
		try (PrintWriter pw = new PrintWriter(new File(OUTPUT_LTL_FILENAME))) {
			for (Parameter p : conf.outputParameters) {
				for (String str : p.temporalProperties()) {
					pw.println(str);
				}
			}
		}

		// all actions
		final List<String> allActions = conf.actions();
		final List<String> allActionDescriptions = conf.actionDescriptions();

		// execution command
		final int recommendedSize = allActionCombinations.size();
		final String nl = " \\\n";

		System.out.println("Run:");
		
		final List<String> builderArgs = new ArrayList<>();
		builderArgs.add(OUTPUT_TRACE_FILENAME);
		builderArgs.add("--actionNames");
		builderArgs.add(String.join(",", allActions));
		if (addActionDescriptions) {
			builderArgs.add("--actionDescriptions");
			builderArgs.add(String.join(",", allActionDescriptions));
		}
		builderArgs.add("--colorRules");
		builderArgs.add(String.join(",", conf.colorRules));
		builderArgs.add("--actionNumber");
		builderArgs.add(String.valueOf(allActions.size()));
		builderArgs.add("--eventNames");
		builderArgs.add(String.join(",", allEvents));
		builderArgs.add("--eventNumber");
		builderArgs.add(String.valueOf(allEvents.size()));
		if (recommendedSize > sizeThreshold) {
			builderArgs.add("--fast");
			System.out.println("# LTL disabled: estimated state number is too large");
		} else {
			builderArgs.add("--ltl");
			builderArgs.add(OUTPUT_LTL_FILENAME);
		}
		builderArgs.add("--actionspec");
		builderArgs.add(OUTPUT_ACTIONSPEC_FILENAME);
		builderArgs.add("--size");
		builderArgs.add(String.valueOf(recommendedSize));
		builderArgs.add("--varNumber");
		builderArgs.add("0");
		builderArgs.add("--result");
		builderArgs.add(gvOutput);
		builderArgs.add("--nusmv");
		builderArgs.add(smvOutput);

		System.out.print("java -jar jars/plant-automaton-generator.jar ");
		for (String arg : builderArgs) {
			if (arg.startsWith("--")) {
				System.out.print(nl + " " + arg + " ");
			} else {
				System.out.print("\"" + arg + "\"");
			}
		}
		System.out.println();

		// parameter limits
		System.out.println("Found parameter boundaries:");
		final Function<Parameter, String> describe = p -> {
			return p.traceName() + " in " + p.limits() + " - " + p;
		};
		for (Parameter p : conf.outputParameters) {
			System.out.println(" output " + describe.apply(p));
		}
		for (Parameter p : conf.inputParameters) {
			System.out.println(" input " + describe.apply(p));
		}
		
		System.out.println(String.format("Input coverage: %.2f%%",
				100.0 * inputCovered.size() / totalInputValues));
		System.out.println(String.format("Output coverage: %.2f%%",
				100.0 * outputCovered.size() / totalOutputValues));
		
		return builderArgs;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		final long time = System.currentTimeMillis();
		final Dataset ds = new Dataset(CONFIGURATION.intervalSec,
				INPUT_DIRECTORY, "", PARAM_SCALES);
		generateScenarios(CONFIGURATION, ds, new HashSet<>(),
				"automaton.gv", "automaton.smv", true, 10, false);
		System.out.println("Execution time: " + (System.currentTimeMillis() - time) + " ms");
	}
}
