/**
 *	Based on original version Copyright 2015 SmartThings
 *	Additions Copyright 2016 Sean Kendall Schneyer
 *	Additions Copyright 2017-2020 Barry A. Burke
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Ecobee Suite Thermostat
 *
 *	Author: SmartThings
 *	Date: 2013-06-13
 *
 *	Updates by Sean Kendall Schneyer <smartthings@linuxbox.org>
 *	Date: 2015-12-23
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com)
 *	Date: 2017 - 2020
 *
 *	See Github Changelog for complete change history
 *
 * <snip>
 *	1.8.00 - Release number sync-up
 *	1.8.01 - General Release
 *	1.8.02 - Fix holdEndDate & schedule during transitions; supportedThermostatModes initialization loophole
 *	1.8.03 - Cleaned up supportedThermostatModes fix
 *	1.8.04 - Better Health Check integration (ST)
 *	1.8.05 - Fixed thermostatMode changes heat-->auto and cool-->auto
 *	1.8.06 - Reworked generateEvent()
 *	1.8.07 - Added debugLevel as dataValue
 *	1.8.08 - New SHPL, using Global Fields instead of State
 *	1.8.09 - Fixed thermostatSetpoint initialization error in auto mode
 *	1.8.10 - Optimizations for HE (no need to update *Display attributes)
 *	1.8.11 - schedule/schedText now sent independently from ESM
 *	1.8.12 - HOTFIX: DeviceWatch fix for hubless ST locations
 *	1.8.13 - Attribute cleanup
 *	1.8.14 - Avoid double resumeProgram() when changing programs
 *	1.8.15 - Fix Hold programs check
 *	1.8.16 - Remove whitespace from thermostatMode & fanMode (Hubitat Dashboard adds extra " " to fanMode)
 *	1.8.17 - Remove debugging "log.error" in generateEvent()
 *	1.8.18 - Added Eco+ event handling
 *	1.8.19 - Added setThermostatHoldHours() entry point
 *	1.8.20 - Allow multiple programs to be adjusted in setProgramSetpoints()
 *	1.8.21 - Fix 1.8.20 changes to work on SmartThings (different version of Groovy from Hubitat)
 *	1.8.22 - Added setSchedule() and schedule attributes for HE 2.2.6.***
 *	1.8.23 - Added (rudimentary) fanSpeed/setFanSpeed() support
 *	1.8.24 - Fixed 'supportedThermostatModes', 'supportedThermostatFanModes', & 'supportedVentModes' for Hubitat 2.3.3 and later
 *	1.9.0a - Added 'fanSpeedOptions' support (new thermostat Capability)
 *	1.9.0b - Hack around Ecobee medium/high idiosyncrasy  (show speeds to match the physical device)
 *	1.9.0c - Added occupancy (occupied/vacant) attribute
 *	1.9.00 - Removed all ST code
 *	1.9.01 - Fixed initial fan speed setting
 *	1.9.02 - Fix thermostatOperatingState initialization error
 *	1.9.03 - Handle additional thermOpStates
 *	1.9.04 - Fixed JsonSlurper null error (line 487)
 *	1.9.05 - Added missing 'set' commands (schedule, fanMode, thermostatMode)
 */
String getVersionNum() 		{ return "1.9.05" }
String getVersionLabel() 	{ return "Ecobee Suite Thermostat, version ${getVersionNum()} on ${getPlatform()}" }
import groovy.json.*
import groovy.transform.Field

metadata {
	definition (
        name:        	"Ecobee Suite Thermostat", 
 //       ocfDeviceType: 	"oic.d.thermostat", //"oic.r.humidity", "oic.r.temperature", "oic.r.motion", "oic.d.humidifier", "oic.d.dehumidifier"],
        namespace:   	"sandood", 
        author:      	"Barry A. Burke (storageanarchy@gmail.com)",		
 //       mnmn:        	"SmartThings", 
 //       vid:         	"SmartThings-smartthings-Z-Wave_Thermostat",
		importUrl:   	"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/devicetypes/sandood/ecobee-suite-thermostat.src/ecobee-suite-thermostat.groovy"
	)
    {		
		capability "Thermostat"
		capability "Actuator"
        capability "Sensor"
		capability "Refresh"
        capability "Thermostat Operating State"
		capability "Thermostat Setpoint"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Motion Sensor"

		attribute 'airAQScore',	    						'NUMBER'
		attribute 'airCO2',	    			    			'NUMBER'
		attribute 'airVOC',	    		    				'NUMBER'
		attribute 'airQAccuracy',	    					'NUMBER'
		attribute 'apiConnected',							'STRING'
		attribute 'autoAway', 								'STRING'
		attribute 'autoHeatCoolFeatureEnabled', 			'STRING'
		attribute 'autoMode', 								'STRING'
		attribute 'auxHeatMode', 							'STRING'
		attribute 'auxMaxOutdoorTemp', 						'NUMBER'
		attribute 'auxOutdoorTempAlert', 					'NUMBER'		// temp
		attribute 'auxOutdoorTempAlertNotify', 				'STRING'
		attribute 'auxRuntimeAlert', 						'NUMBER'		// time
		attribute 'auxRuntimeAlertNotify', 					'STRING'
		attribute 'auxOutdoorTempAlertNotifyTechnician', 	'STRING'
		attribute 'auxRuntimeAlertNotifyTechnician', 		'STRING'
		attribute 'backlightOffDuringSleep', 				'STRING'
		attribute 'backlightOffTime', 						'NUMBER'
		attribute 'backlightOnIntensity', 					'NUMBER'
		attribute 'backlightSleepIntensity', 				'NUMBER'
		attribute 'brand', 									'STRING'
        attribute 'climatesList', 							'JSON_OBJECT'	// USAGE: List theClimates = stat.currentValue('climatesList')[1..-2].tokenize(', ')
		attribute 'coldTempAlert', 							'NUMBER'
		attribute 'coldTempAlertEnabled', 					'STRING'		// or boolean?
		attribute 'compressorProtectionMinTemp', 			'NUMBER'
		attribute 'compressorProtectionMinTime', 			'NUMBER'
		attribute 'condensationAvoid', 						'STRING'		// boolean
		attribute 'coolAtSetpoint', 						'NUMBER'
		attribute 'coolDifferential', 						'NUMBER'
		attribute 'coolMaxTemp', 							'NUMBER'		// Read Only
		attribute 'coolMinTemp', 							'NUMBER'		// Read Only
		attribute 'coolMode', 								'STRING'
		attribute 'coolRange', 								'STRING'
		attribute 'coolRangeHigh', 							'NUMBER'
		attribute 'coolRangeLow', 							'NUMBER'
		attribute 'coolStages', 							'NUMBER'		// Read Only
		attribute 'coolingLockout', 						'STRING'
		attribute 'coolingSetpointMax', 					'NUMBER'
		attribute 'coolingSetpointMin', 					'NUMBER'
		attribute 'coolingSetpointRange', 					'vector3'
		attribute 'coldTempAlert', 							'NUMBER'
		attribute 'coldTempAlertEnabled', 					'STRING'
		attribute 'currentProgram',							'STRING'		// Read only
		attribute 'currentProgramId',						'STRING'		// Read only
		attribute 'currentProgramName', 					'STRING'		// Read only
        attribute 'currentProgramOwner', 					'STRING'		// Read only
        attribute 'currentProgramType', 					'STRING'		// Read only
		attribute 'debugEventFromParent',					'STRING'		// Read only
		attribute 'debugLevel', 							'NUMBER'		// Read only - changed in preferences
		attribute 'decimalPrecision', 						'NUMBER'		// Read only - changed in preferences
		attribute 'dehumidifierLevel', 						'NUMBER'		// should be same as dehumiditySetpoint
		attribute 'dehumidifierMode', 						'STRING'
		attribute 'dehumidifyOvercoolOffset', 				'NUMBER'
		attribute 'dehumidifyWhenHeating', 					'STRING'
		attribute 'dehumidifyWithAC', 						'STRING'
        attribute 'dehumidityLevel', 						'NUMBER'
		attribute 'dehumiditySetpoint', 					'NUMBER'
		attribute 'disableAlertsOnIdt', 					'STRING'
		attribute 'disableHeatPumpAlerts', 					'STRING'
		attribute 'disablePreCooling', 						'STRING'
		attribute 'disablePreHeating', 						'STRING'
		attribute 'drAccept', 								'STRING'
        //attribute 'ecoPlus',								'STRING'
		attribute 'ecobeeConnected', 						'STRING'
		attribute 'eiLocation', 							'STRING'
		attribute 'electricityBillCycleMonths', 			'STRING'
		attribute 'electricityBillStartMonth', 				'STRING'
		attribute 'electricityBillingDayOfMonth', 			'STRING'
		attribute 'enableElectricityBillAlert', 			'STRING'
		attribute 'enableProjectedElectricityBillAlert', 	'STRING'
		attribute 'equipmentOperatingState', 				'STRING'
		attribute 'equipmentStatus', 						'STRING'
		attribute 'fanControlRequired', 					'STRING'
		attribute 'fanMinOnTime', 							'NUMBER'
		attribute 'fanSpeed',								'STRING'		// One of 'low', 'medium', 'high', and 'optimized'
		attribute 'fanSpeedOptions',						'JSON_OBJECT'	// USAGE: List fanSpeedOptions = new JsonSlurper().parseText(stat.currentValue('fanSpeedOptions'))
		attribute 'features',								'STRING'
		attribute 'followMeComfort', 						'STRING'
		attribute 'groupName', 								'STRING'
		attribute 'groupRef', 								'STRING'
		attribute 'groupSetting', 							'STRING'
		attribute 'hasBoiler', 								'STRING'		// Read Only
		attribute 'hasDehumidifier',						'STRING'		// Read Only
		attribute 'hasElectric', 							'STRING'		// Read Only
		attribute 'hasErv', 								'STRING'		// Read Only
		attribute 'hasForcedAir', 							'STRING'		// Read Only
		attribute 'hasHeatPump', 							'STRING'		// Read Only
		attribute 'hasHrv', 								'STRING'		// Read Only
		attribute 'hasHumidifier', 							'STRING'		// Read Only
		attribute 'hasUVFilter', 							'STRING'		// Read Only
		attribute 'heatAtSetpoint', 						'NUMBER'
		attribute 'heatCoolMinDelta', 						'NUMBER'
		attribute 'heatDifferential', 						'NUMBER'
		attribute 'heatMaxTemp', 							'NUMBER'		// Read Only
		attribute 'heatMinTemp', 							'NUMBER'		// Read Only
		attribute 'heatMode', 								'STRING'
		attribute 'heatPumpGroundWater', 					'STRING'		// Read Only
		attribute 'heatPumpReversalOnCool', 				'STRING'
		attribute 'heatRange', 								'STRING'
		attribute 'heatRangeHigh', 							'NUMBER'
		attribute 'heatRangeLow', 							'NUMBER'
		attribute 'heatStages', 							'NUMBER'		// Read Only
		attribute 'heatingSetpointMax',	 					'NUMBER'
		attribute 'heatingSetpointMin', 					'NUMBER'
		attribute 'heatingSetpointRange', 					'vector3'
		attribute 'holdAction', 							'STRING'
		attribute 'holdEndsAt', 							'STRING'
        attribute 'holdEndDate', 							'STRING'
		attribute 'holdStatus', 							'STRING'
		attribute 'hotTempAlert', 							'NUMBER'
		attribute 'hotTempAlertEnabled', 					'STRING'
		attribute 'humidifierMode', 						'STRING'
        attribute 'humidity', 								'NUMBER'
		attribute 'humidityAlertNotify', 					'STRING'
		attribute 'humidityAlertNotifyTechnician', 			'STRING'
		attribute 'humidityHighAlert', 						'NUMBER'		// %
		attribute 'humidityLowAlert', 						'NUMBER'
		attribute 'humiditySetpoint', 						'NUMBER'
		attribute 'humiditySetpointDisplay',				'STRING'
		attribute 'identifier', 							'STRING'
		attribute 'installerCodeRequired', 					'STRING'
		attribute 'isRegistered', 							'STRING' 
		attribute 'isRentalProperty', 						'STRING'
		attribute 'isVentilatorTimerOn', 					'STRING'
		attribute 'lastHoldType', 							'STRING'
		attribute 'lastModified', 							'STRING' 
		attribute 'lastOpState', 							'STRING'		// keeps track if we were most recently heating or cooling
		attribute 'lastPoll', 								'STRING'
        attribute 'lastRunningMode', 						'STRING'		// for Google Home
		attribute 'lastServiceDate', 						'STRING'
		attribute 'locale', 								'STRING'
		attribute 'logo', 									'STRING'
		attribute 'maxSetBack', 							'NUMBER'
		attribute 'maxSetForward', 							'NUMBER'
        attribute 'microphoneEnabled', 						'STRING'		// From Audio object (boolean)
		attribute 'mobile', 								'STRING'
		attribute 'modelNumber', 							'STRING' 
		attribute 'monthlyElectricityBillLimit', 			'STRING'
		attribute 'monthsBetweenService', 					'STRING'
		attribute 'motion', 								'STRING'
		attribute 'name', 									'STRING'
        attribute "nextCoolingSetpoint", 					'NUMBER'		// Next 3 are for Smart Vents handling of (Smart Recovery)
        attribute "nextHeatingSetpoint", 					'NUMBER'		// "
        attribute 'nextProgramName', 						'STRING'		// ""
        attribute "notificationMessage", 					'STRING'
        attribute 'playbackVolume', 						'NUMBER'		// From Audio object
		attribute "occupancy", 								'ENUM', ['occupied', 'vacant']
		attribute 'programsList', 							'STRING'		// USAGE: List programs = new JsonSlurper().parseText(stat.currentValue('programsList'))
		attribute 'quickSaveSetBack', 						'NUMBER'
		attribute 'quickSaveSetForward', 					'NUMBER'
		attribute 'randomStartDelayCool', 					'STRING'
		attribute 'randomStartDelayHeat', 					'STRING'
		attribute 'remindMeDate', 							'STRING'
		attribute 'schedText', 								'STRING'
		attribute 'schedule', 								'STRING'		// 'currentProgramName' + 'until XXXX' if is a Hold
		attribute 'scheduledProgram',						'STRING'
		attribute 'scheduledProgramId',						'STRING'
		attribute 'scheduledProgramName', 					'STRING'
        attribute 'scheduledProgramOwner', 					'STRING'
        attribute 'scheduledProgramType', 					'STRING'
		attribute 'serviceRemindMe', 						'STRING'
		attribute 'serviceRemindTechnician', 				'STRING'
		attribute 'smartCirculation', 						'STRING'		// not implemented by E3, but by this code it is
		attribute 'soundAlertVolume', 						'NUMBER'		// From Audio object
		attribute 'soundTickVolume', 						'NUMBER'		// From Audio object
		attribute 'stage1CoolingDifferentialTemp', 			'NUMBER'
		attribute 'stage1CoolingDissipationTime', 			'NUMBER'
		attribute 'stage1HeatingDifferentialTemp', 			'NUMBER'
		attribute 'stage1HeatingDissipationTime', 			'NUMBER'
		attribute 'statHoldAction', 						'STRING'
		attribute 'supportedThermostatFanModes', 			'JSON_OBJECT' 	// USAGE: List theFanModes = new JsonSlurper().parseText(stat.currentValue('supportedThermostatFanModes'))
		attribute 'supportedThermostatModes', 				'JSON_OBJECT' 	// USAGE: List theModes = new JsonSlurper().parseText(stat.currentValue('supportedThermostatModes'))
		attribute 'supportedVentModes', 					'JSON_OBJECT' 	// USAGE: List theVentModes = new JsonSlurper().parseText(stat.currentValue('supportedVentModes'))
		attribute 'tempAlertNotify', 						'STRING'
		attribute 'tempAlertNotifyTechnician', 				'STRING'
		attribute 'tempCorrection', 						'NUMBER'
		attribute 'temperatureScale', 						'STRING'
		attribute 'thermostatHold', 						'STRING'
		attribute 'thermostatOperatingStateDisplay', 		'STRING'
		attribute 'thermostatSetpoint',						'NUMBER'
		attribute 'thermostatSetpointMax', 					'NUMBER'
		attribute 'thermostatSetpointMin', 					'NUMBER'
		attribute 'thermostatSetpointRange', 				'vector3'
		attribute 'thermostatStatus',						'STRING'
		attribute 'thermostatTime', 						'STRING'
		attribute 'timeOfDay', 								'enum', ['day', 'night']
		attribute 'userAccessCode', 						'STRING'		// Read Only
		attribute 'userAccessSetting', 						'STRING'		// Read Only
		attribute 'vent', 									'STRING'		// same as 'ventMode'
		attribute 'ventMode', 								'STRING'		// Ecobee actually calls it only 'vent'
		attribute 'ventilatorDehumidify', 					'STRING'
		attribute 'ventilatorFreeCooling', 					'STRING'
		attribute 'ventilatorMinOnTime', 					'NUMBER'
		attribute 'ventilatorMinOnTimeAway', 				'NUMBER'
		attribute 'ventilatorMinOnTimeHome', 				'NUMBER'
		attribute 'ventilatorOffDateTime', 					'STRING'		// Read Only
		attribute 'ventilatorType', 						'STRING'		// Read Only ('none', 'ventilator', 'hrv', 'erv')
        attribute 'voiceEngines', 							'JSON_OBJECT'	// From Audio object
		attribute 'weatherDewpoint', 						'NUMBER'
		attribute 'weatherHumidity', 						'NUMBER'
		attribute 'weatherPressure', 						'NUMBER'
		attribute 'weatherSymbol', 							'STRING'
		attribute 'weatherTemperature', 					'NUMBER'
        attribute 'weatherTempLowForecast', 				'STRING'		// "lowToday,low1Day,low2Day,low3Day,low4Day" in F (for Smart Humidity Helper)
		attribute 'wifiOfflineAlert', 						'STRING'
        // These are intentionally last (for now)
        
        attribute "alertsUpdated", 							'STRING'
        attribute "audioUpdated", 							'STRING'
        attribute "climatesUpdated", 						'STRING'
        attribute "curClimRefUpdated", 						'STRING'
        attribute "equipUpdated", 							'STRING'
        attribute "extendRTUpdated", 						'STRING'
        attribute "eventsUpdated", 							'STRING'
        attribute "locationUpdated", 						'STRING'
        attribute "programUpdated", 						'STRING'
        attribute "runtimeUpdated", 						'STRING'
        attribute "scheduleUpdated", 						'STRING'
        attribute "sensorsUpdated", 						'STRING'
        attribute "settingsUpdated", 						'STRING'
        attribute "statInfoUpdated", 						'STRING'
        attribute "weatherUpdated", 						'STRING'
        

		command "asleep", 					[]						// We cannot overload the internal Java/Groovy definition of 'sleep()'
		// command "auto", 					[]
		command "auxHeatOnly", 				[]
		command "awake", 					[]
		command "away", 					[]
        command "cancelDemandResponse",		[]
		command "cancelReservation", 		[]
		command "cancelVacation", 			[]
		// command "cool", 					[]
		command "deleteVacation", 			[]
		command "doRefresh", 				[]						// internal use by the refresh button
		command "emergency", 				[]	
		// command "emergencyHeat", 		[]
		// command "fanAuto", 				[]
		// command "fanCirculate", 			[]
		command "fanOff", 					[]						// Missing from the Thermostat standard capability set
		// command "fanOn", 				[]
		command "forceRefresh", 			[]	
		command "generateEvent"
		// command "heat", 					[]
		command "home", 					[]	
		command "lowerSetpoint", 			[]	
		command "makeReservation", 			[]	
        command	"microphoneOff",			[]
        command "microphoneOn",				[]
		command "night", 					[]						// this is probably the appropriate SmartThings device command to call, matches ST mode
		command "noOp", 					[]						// Workaround for formatting issues
		// command "off", 					[]						// redundant - should be predefined by the Thermostat capability
		command "present", 					[]	
		command "raiseSetpoint", 			[]	
		command "resumeProgram", 			['STRING']
		command "setCoolingSetpointDelay",	[]
       	command "setDehumidifierMode", 		[[name:'Dehumidifier Mode*', type:'ENUM', description:'Select a (valid) Mode',
										  	  constraints: ['auto','off','on']]]
       	command "setDehumiditySetpoint",	[[name:'Dehumidity Setpoint*', type:'NUMBER', description:'Dehumidifier RH% setpoint (0-100)']]
       	command "setEcobeeSetting", 		[[name:'Ecobee Setting Name*', type:'STRING', description:'Name of setting to change'],
										 	 [name:'Ecobee Setting Value*', type:'STRING', description:'New value for this setting']]
		command "setFanMinOnTime", 			[[name:'Fan Min On Time*', type:'NUMBER', description:'Minimum fan minutes/hour (0-55)']]
		command "setFanMinOnTimeDelay", 	[]
		command "setFanSpeed",				[[name:'Fan Speed', type:'ENUM', description:'Select desired fan speed', 
											  constraints: ['low', 'medium', 'high', 'optimized']]]
		command "setHeatingSetpointDelay",	[]
       	command	"setHumidifierMode", 		[[name:'Humidifier Mode*', type:'ENUM', description:'Select a (valid) Mode',
											  constraints: ['auto','off','on']]]
		command "setHumiditySetpoint", 		[[name:'Humidity Setpoint*', type:'NUMBER', description:'Humidifier RH% setpoint (0-100)']]
		command "setProgramSetpoints", 		[[name:'Program Name*', type:'STRING', description:'Program to change'],
											 [name:'Heating Setpoint*', type:'NUMBER', description:'Heating setpoint temperature'],
											 [name:'Cooling Setpoint*', type:'NUMBER', description:'Cooling setpoint temperature']]
		command "setSchedule",				[[name: 'Schedule*', type: 'JSON_OBJECT']]
		command "setThermostatFanMode",		[[name: 'Fan Mode*', type: 'ENUM', constraints: ['auto', 'circulate', 'off', 'on']]]
		command "setThermostatMode",			[[name: 'Thermostat Mode*', type: 'ENUM', constraints: ['auto', 'auxHeat', 'cool', 'heat', 'off']]]
		command "setThermostatHoldHours",   [[name:'Hold Hours', type:'NUMBER', description:'Default Hours for an Hold Type == Hold Hours (use zero to default to ES Manager setting)']]
		command "setThermostatProgram", 	[[name:'Program Name*', type:'STRING', description:'Desired Program'],
											 [name:'Hold Type*', type:'ENUM', description:'Select an option',
											  constraints: ['indefinite', 'nextTransition', 'holdHours']],
											 [name:'Hold Hours', type:'NUMBER', description:'Hours to Hold (if Hold Type == Hold Hours]']]
  		command "setVacationFanMinOnTime",	[[name:'Fan Min On Time for Vacation Hold*', type:'NUMBER', description:'Minimum fan minutes/hour (0-55)']]
		command "wakeup", 					[]
	}

	simulator { }
	
    preferences {
        input(name: "myHoldType", type: "enum", title: "Hold Type", description: 
			  "When changing temperature or program, use Permanent, Temporary, Hourly, Parent setting or Thermostat setting hold type? (Default: Ecobee Suite Manager's setting)",
			  required: false, options: ["Permanent", "Temporary", "Hourly", "Parent", "Thermostat"])
        input(name: "myHoldHours", type: "enum", title: "Hourly Hold Time", description: "If Hourly hold, how many hours do you want to hold? (Default: Ecobee Suite Manager's setting)", 
			  required: false, options: ['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','28','32','36','40','44','48','56','64','72'])
        input(name: "smartAuto", type: "bool", title: "Smart Auto Temp Adjust", description: "This flag allows the temperature to be adjusted manually when the thermostat " +
			  "is in Auto mode. An attempt to determine if the heat or cool setting should be changed is made automatically.", required: false)
		input(name: "dummy", type: "text", title: "${getVersionLabel()}", description: " ", required: false)
	}
}

// parse events into attributes
def parse(String description) {
	LOG( "parse() --> Parsing '${description}'" )
}

def refresh(force=false) {
	// No longer require forcePoll on every refresh - just get whatever has changed
	LOG("refresh() - calling pollChildren ${force?'(forced)':''}, deviceId = ${getDeviceId()}",2,null,'info')
	parent.pollChildren(getDeviceId(), force) // tell parent to just poll me silently -- can't pass child/this for some reason
}
def doRefresh() {
	// Pressing refresh within 6 seconds of the prior refresh completing will force a complete poll of the Ecobee cloud - otherwise changes only
	refresh(state.lastDoRefresh?((now()-state.lastDoRefresh)<6000):false)
	sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
	runIn(2, 'resetRefreshButton', [overwrite: true])
	resetUISetpoints()
	state.lastDoRefresh = now()	// reset the timer after the UI has been updated
}
def resetRefreshButton() {
	sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
}
def forceRefresh() {
	refresh(true)
}

def installed() {
	LOG("${device.label} being installed",2,null,'info')
	if (device.label?.contains('TestingForInstall')) return	// we're just going to be deleted in a second...
	updated()
}

def uninstalled() {
	LOG("${device.label} being uninstalled",2,null,'info')
}

def updated() {
	//getHubPlatform()
	LOG("${getVersionLabel()} updated",1,null,'trace')
	state.supportedThermostatModes = []
	state.supportedVentModes = []
	
	if (!device.displayName.contains('TestingForInstall')) {
	// Try not to get hung up in the Health Check so that ES Manager can delete the temporary device
		sendEvent(name: 'checkInterval', value: 3900, displayed: false, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
	} else {
		return
	}
	
	resetUISetpoints()
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
	state.defaultHoldHours = settings.myHoldHours
	state.version = getVersionLabel()
    updateDataValue("myVersion", getVersionLabel())
	runIn(2, 'forceRefresh', [overwrite: true])
}

def poll() {
	LOG("Executing 'poll' using parent SmartApp", 2, null, 'info')
	parent.pollChildren(getDeviceId(),false) // tell parent to just poll me silently -- can't pass child/this for some reason
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
def ping() {
	LOG("Health Check ping - apiConnected: ${device.currentValue('apiConnected', true)}, ecobeeConnected: ${device.currentValue('ecobeeConnected', true)}, checkInterval: ${device.currentValue('checkInterval', true)} seconds",1,null,'warn')
	parent.pollChildren(getDeviceId(),true)		// forcePoll just me
}
def generateEvent(Map updates) {
	//log.error "generateEvent(Map): ${updates}"
	generateEvent([updates])
}
def generateEvent(List updates) {
	//log.debug "updates: ${updates}"
	//if (!state.version || (state.version != getVersionLabel())) updated()
    String myVersion = getDataValue("myVersion")
    if (!myVersion || (myVersion != getVersionLabel())) updated()
	def startMS = now()
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("generateEvent(): parsing data ${updates}", 1)
	//LOG("Debug level of parent: ${getParentSetting('debugLevel')}", 4, null, "debug")
	def linkText = device.displayName
	def tu = getTemperatureScale()
	boolean isMetric = (tu == 'C')
	boolean forceChange = false
	def ps
	boolean isIOS, isAndroid
	boolean updateTempRanges = false
	def precision = device.currentValue('decimalPrecision')
	if (precision == null) precision = isMetric ? 1 : 0
	int objectsUpdated = 0
	List supportedThermostatModes = []
	if (state.supportedThermostatModes == []) {				// attribute updates will continuously update this if config changes
		// Initialize for those that are updating the DTH with this version
		supportedThermostatModes = ['off']
		if (device.currentValue('heatMode', true) == 'true') supportedThermostatModes += ['heat']
		if (device.currentValue('coolMode', true) == 'true') supportedThermostatModes += ['cool']
		if (device.currentValue('autoMode', true) == 'true') supportedThermostatModes += ['auto']
		if (device.currentValue('auxHeatMode', true) == 'true') supportedThermostatModes += ['auxHeatOnly', 'emergency heat']
		state.supportedThermostatModes = supportedThermostatModes.sort(false)
		String stmJSON = new groovy.json.JsonBuilder(state.supportedThermostatModes).toString()
		sendEvent(name: "supportedThermostatModes", value: stmJSON, displayed: false, isStateChange: true)
		sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false, isStateChange: true)
	}
	supportedThermostatModes = state.supportedThermostatModes

	if (device.currentValue('autoHeatCoolFeatureEnabled') == null) sendEvent(name: 'autoHeatCoolFeatureEnabled', value: device.currentValue('autoMode'), isStateChange: true, displayed: false)
	def vType = device.currentValue('ventilatorType')
	if ((vType != null) && (vType != 'none')) {
		if ((state.supportedVentModes == []) || (state.supportedVentModes.size() == 1)){
			state.supportedVentModes = new groovy.json.JsonSlurper().parseText(ventModes()).sort(false)
			sendEvent(name: 'supportedVentModes', value: state.supportedVentModes, displayed: false, isStateChange: true)
		}
	} else {
		if (state.supportedVentModes == []) {
			state.supportedVentModes = ['off']
			sendEvent(name: 'supportedVentModes', value: "[\"off\"]", displayed: false, isStateChange: true)
		}
	}

	String tMode = device.currentValue('thermostatMode', true)
	String lRunMode = (tMode == 'auto') ? getDataValue("lastRunningMode") : ''
	if(updates) {
		updates.each { update ->
			update.each { name, value ->
				objectsUpdated++
				if (debugLevelFour) LOG("generateEvent() - In each loop: object #${objectsUpdated} name: ${name} value: ${value}", 1, null, 'trace')

				String tempDisplay = ""
				def eventFront = [name: name, linkText: linkText, handlerName: name]
				def event = [:]
				String sendValue = value.toString()
				boolean isChange = isStateChange(device, name, sendValue)

				switch (name) {
					case 'forced':
						forceChange = (sendValue == 'true')
						break;

					case 'heatingSetpoint':
						if (isChange || forceChange) {
							// We have to send for backwards compatibility
							if ((tMode == 'heat') || (lRunMode && (lRunMode == 'heat'))) {
								sendEvent(name: 'thermostatSetpoint', value: sendValue, unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: false)
								objectsUpdated++
							} else if (tMode == 'auto') {
								def ncCsp = device.currentValue('coolingSetpoint', true)
								def avg = roundIt(((value.toBigDecimal() + ncCsp.toBigDecimal()) / 2.0), precision.toInteger())
								sendEvent(name: 'thermostatSetpoint', value: avg.toString(), unit: tu, descriptionText: "Thermostat setpoint average is ${avg}°${tu}", displayed: true)
								objectsUpdated++
							}
							def heatDiff 
                            if ((device.currentValue("currentProgram") == "Vacation") && (device.currentValue("currentProgramId") == 'null')) {
                                // Thermostat defaults to 1.5F when in Vacation mode
                                heatDiff = 1.5
                            } else {
                                heatDiff = device.currentValue('heatDifferential', true)
                            }
							if (!heatDiff || (heatDiff == 'null') || (heatDiff == "")) heatDiff = 0.0
							String heatAt = roundIt((value.toBigDecimal() - heatDiff.toBigDecimal()), precision.toInteger()).toString()
							sendEvent(name: 'heatAtSetpoint', value: heatAt, unit: tu, displayed: false)
							objectsUpdated++
							event = eventFront + [value: sendValue, unit: tu, descriptionText: "Heating setpoint is ${sendValue}°${tu}"]
						}
						break;

					case 'heatingSetpointDisplay':
						objectsUpdated--
						break;

					case 'coolingSetpoint':
						if (isChange || forceChange) {
							//log.debug "coolingSetpoint Change (${isChange}/${forceChange}): ${sendValue}"
							//LOG("coolingSetpoint: ${sendValue}",3,null,'info')
							if ((tMode == 'cool') || (lRunMode && (lRunMode == 'cool'))) { 
								sendEvent(name: 'thermostatSetpoint', value: sendValue, unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: false)
								objectsUpdated++
							} else if (tMode == 'auto') {
								def ncHsp = device.currentValue('heatingSetpoint', true)
								def avg = roundIt(((value.toBigDecimal() + ncHsp.toBigDecimal()).toBigDecimal() / 2.0).toBigDecimal(), precision.toInteger())
								sendEvent(name: 'thermostatSetpoint', value: avg.toString(), unit: tu, descriptionText: "Thermostat setpoint average is ${avg}°${tu}", displayed: true)
								objectsUpdated++
							}
							def coolDiff
                            if ((device.currentValue("currentProgram") == "Vacation") && (device.currentValue("currentProgramId") == "null")) {
                                // Thermostat defaults to 1.5F when in Vacation mode
                                coolDiff = 1.5
                            } else {
                                coolDiff = device.currentValue('coolDifferential', true)
                            }
							if (!coolDiff || (coolDiff == 'null') || (coolDiff == "")) coolDiff = 0.0
							String coolAt = roundIt((value.toBigDecimal() + coolDiff.toBigDecimal()), precision.toInteger()).toString()
							sendEvent(name: 'coolAtSetpoint', value: coolAt, unit: tu,displayed: false)
							objectsUpdated++
							event = eventFront + [value: sendValue, unit: tu, descriptionText: "Cooling setpoint is ${sendValue}°${tu}", displayed: true]
						}
						break;

					case 'coolingSetpointDisplay':
						objectsUpdated--
						break;

					case 'thermostatSetpoint':
						if (isChange || forceChange) {
							def displayValue = isMetric ? sendValue : roundIt(value, 0) // Truncate the decimal point
							event = eventFront + [value: displayValue.toString(), unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: true]
						}
						break;

					case 'weatherTemperature':
						if (isChange) event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
						break;

					case 'weatherHumidity':
						if (isChange) event = eventFront + [value: sendValue, unit: '%', descriptionText: "Outdoor humidity is ${sendValue}%", isStateChange: true, displayed: true]
						break;

					case 'weatherDewpoint':
						//log.debug "weatherDewpoint ${sendValue}"
						if (isChange) event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
						break;

					case 'weatherPressure':
						def pScale = isMetric ? 'mbar' : 'inHg'
						if (isChange) event = eventFront + [value: sendValue, unit: pScale, descriptionText: "Barometric Pressure is ${sendValue}${pScale}", isStateChange: true, displayed: true]
						break;

					case 'weatherTempLowForecast':
						if (isChange) event = eventFront + [value: sendValue, unit: 'F', descriptionText: "", isStateChange: true, displayed: false]
						break;

					case 'temperature':
						String ncTos = device.currentValue('thermostatOperatingState', true)
						if (ncTos == 'offline') {
							tempDisplay = '451°'		// As in Fahrenheit 451
						} else {
							if (sendValue && (sendValue != 'null') && (sendValue != "")) {			// don't send null temperature values
								def dValue = sendValue?.toBigDecimal()
								// Generate the display value that will preserve decimal positions ending in 0
								if (precision == 0) {
									tempDisplay = roundIt(dValue, 0).toString() + '°'
									sendValue = roundIt(dValue, 0).toInteger()
								} else {
									tempDisplay = String.format( "%.${precision.toInteger()}f", roundIt(dValue, precision.toInteger())) + '°'
								}
								//redisplay = (device.currentValue('temperatureDisplay') == '451°'
								if (isChange || forceChange) {
									event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), displayed: true]
									LOG("Temperature is ${tempDisplay}${tu}", 1, null, 'info')
								}
							}
						}
						break;

					case 'thermostatOperatingState':
                        if (sendValue != "") {
                            // A little tricky, but this is how we display Smart Recovery within the stock Thermostat multiAttributeTile
                            // thermostatOperatingStateDisplay has the same values as thermostatOperatingState, plus "heating (smart recovery)"
                            // and "cooling (smart recovery)". We separately update the standard thermostatOperatingState attribute.
                            //log.debug "thermostatOperatingState: ${sendValue}"
                            boolean displayDesc
                            String descText
                            String realValue
                            if (sendValue.contains('(')) {
                                displayDesc = true				// only show this update ThermOpStateDisplay if we are in Smart Recovery
                                if (sendValue.contains('mart')) {
                                    descText = 'in Smart Recovery'	// equipmentOperatingState will show what is actually running
                                    realValue = sendValue.take(7)
                                } else if (sendValue.contains('ver')) {
                                    descText = 'Overcooling to Dehumidify'
                                    realValue = 'cooling'	// this gets us to back to just heating/cooling
								} else if (sendValue.startsWith('fan') || sendValue.startsWith('idle')) {
									realValue = sendValue.startsWith('fan') ? 'fan only' : 'idle' 
                                    if 		(sendValue.contains('deh')) descText = 'Dehumidifying'
                                    else if (sendValue.contains('hum')) descText = 'Humidifying'
                                    else if (sendValue.contains('eco')) descText = 'Economizing'
                                    else if (sendValue.contains('ven')) descText = 'Ventilating'
                                    else if (sendValue.contains('hot w')) descText = 'Heating Water'
                                } else if (sendValue.contains('hot w')) {
                                    descText = 'Heating Water'
                                    realValue = sendValue.contains('heating') ? 'heating' : 'idle'
                                }
                            } else {
                                displayDesc = false				// hide this message - is redundant with EquipOpState
                                descText = sendValue.capitalize()
                                realValue = sendValue
                            }
							//if (descText = "") log.debug "sv: ${sendValue}, rv: ${realValue}, dt: ${descText}"
                            if ((forceChange || isStateChange(device, 'thermostatOperatingStateDisplay', sendValue))) {
                                //log.debug "updating thermostatOperatingStateDisplay with ${sendValue}"
                                sendEvent(name: "thermostatOperatingStateDisplay", value: sendValue, descriptionText: "Thermostat Operating State is ${descText}", linkText: linkText,
                                          handlerName: "${name}Display", isStateChange: true, displayed: displayDesc)
                                LOG("HVAC system is ${sendValue}", 1, null, 'info')
                                objectsUpdated++
                            }

                            // now update thermostatOperatingState - is limited by API to idle, fan only, heating, cooling, pending heat, pending cool, ventilator only
                            if (forceChange || isStateChange(device, name, realValue)) {
                                // First, check if we need to change the Heating at/Heating to temperature values
                                if (realValue.contains('heat')) {	// heating, aux heat, emergency heat are all the same
                                    // heatingSetpoint should display actual setpoint for "Heating to..."
                                    String heatSetp = device.currentValue('heatingSetpoint', true).toString()
                                    sendEvent(name: 'thermostatSetpoint', value: heatSetp, unit: tu, descriptionText: "Thermostat setpoint is ${heatSetp}°${tu}", displayed: true) // For Google Home
                                    sendEvent(name: 'lastRunningMode', value: 'heat', displayed: false) // For Google Home
                                    updateDataValue('lastRunningMode', 'heat')
                                    state.lastRunningMode = 'heat'
                                }
                                if (realValue.startsWith('cool')) {
                                    // coolingSetpoint should display actual setpoint for "Cooling to..."
                                    String coolSetp = device.currentValue('coolingSetpoint', true).toString()
                                    sendEvent(name: 'thermostatSetpoint', value: coolSetp, unit: tu, descriptionText: "Thermostat setpoint is ${coolSetp}°${tu}", displayed: true) // For Google Home
                                    sendEvent(name: 'lastRunningMode', value: 'cool', displayed: false) // For Google Home
                                    updateDataValue('lastRunningMode', 'cool')
                                    state.lastRunningMode = 'cool'
                                }
                                event = eventFront + [value: realValue, descriptionText: "Thermostat Operating State is ${realValue}", displayed: false]

                                // Keep track of whether we were last heating or cooling
                                def lastOpState = device.currentValue('thermostatOperatingState', true)
                                if (lastOpState && (lastOpState.contains('ea') || lastOpState.contains('oo'))) sendEvent(name: 'lastOpState', value: lastOpState, displayed: false)
                            }
                        }
                        break;

					case 'equipmentOperatingState':
						if ((sendValue != 'off') && (tMode == 'off')) {
							sendValue = 'off'
							isChange = isStateChange(device, name, sendValue)
						}
						if (isChange || forceChange) {
							if (sendValue == 'off') {
								// force thermostat to appear idle when it is off
								def lastOpState = device.currentValue('thermostatOperatingState', true)
								if (lastOpState.contains('ea') || lastOpState.contains('oo')) sendEvent(name: 'lastOpState', value: lastOpState, displayed: false)
								sendEvent(name: 'thermostatOperatingState', value: 'idle', descriptionText: 'Thermostat Operating State is idle', displayed: true)
								objectsUpdated++
								if (true || isIOS) {
									// Android doesn't handle 'off' - update only the display
									sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', descriptionText: 'Thermostat Operating State is off', displayed: false)
									objectsUpdated++
								}
							}
							String descText = sendValue.endsWith('deh') ? sendValue.replace('deh', '& dehumidifying') : (sendValue.endsWith('hum') ? sendValue.replace('hum', '& humidifying') : sendValue)
							if (!descText.startsWith('heat pump')) {
								descText = descText.replace('heat ','heating ')
								descText = descText.replace('cool ','cooling ')
							}
							descText = descText.replace('1', '(stage 1)').replace('2', '(stage 2)').replace('3', '(stage 3)').replace('fier', 'fying')
							event = eventFront + [value: sendValue, descriptionText: "Equipment is ${descText}", isStateChange: isChange, displayed: true]
						}
						break;

					case 'equipmentStatus':
						if (isChange || forceChange) {
							String descText = (sendValue == 'idle') ? 'Equipment is idle' : ((sendValue == 'offline') ? 'Equipment is offline' : "Equipment is running " + sendValue)
							event = eventFront +  [value: sendValue, descriptionText: descText, displayed: false]
						}
						break;

					case 'lastPoll':
						if (isChange) event = eventFront + [value: sendValue, descriptionText: "Poll: " + sendValue, isStateChange: true, displayed: debugLevel(4)]
						break;

					case 'humidity':
						if (isChange || forceChange) {
							def humSetpoint = device.currentValue('humiditySetpoint', true)
							// if (humSetpoint == null) humSetpoint = 0
							String setpointText = (!humSetpoint || (humSetpoint == 'null') || (humSetpoint == 0)) ? '' : " (setpoint: ${humSetpoint}%)"
							event = eventFront + [value: sendValue, unit: '%', descriptionText: "Humidity is ${sendValue}%${setpointText}", displayed: true]
						}
						break;

					case 'humiditySetpoint':
						if (isChange) {
							log.debug "humiditySetpoint: ${sendValue}"
							event = eventFront + [value: sendValue, unit: '%', descriptionText: "Humidifier setpoint is ${sendValue}%", isStateChange: true, displayed: true]
						}
						break;

					case 'dehumiditySetpoint':
						if (isChange) {
							sendEvent(name: 'dehumidifierLevel', value: sendValue, unit: '%', descriptionText: "Dehumidifier level is ${sendValue}%", isStateChange: true, displayed: false)
							sendEvent(name: 'dehumidityLevel', value: sendValue, unit: '%', descriptionText: "Dehumidity level is ${sendValue}%", isStateChange: true, displayed: false)
							event = eventFront + [value: sendValue, unit: '%', descriptionText: "Dehumidifier setpoint is ${sendValue}%", isStateChange: true, displayed: true]
						}
						break;

//					case 'humiditySetpointDisplay':
//						break;

					case 'currentProgramName':
						//LOG("currentProgramName: ${sendValue}", 1, null, 'debug')
						// always update the button states, even if the value didn't change
						String progText = ''
						String priorProgramName = device.currentValue('currentProgramName', true)
						if (sendValue == 'Vacation') {
							progText = 'Climate is Vacation'
						} else if (sendValue.startsWith('Hold: Ec')) {
							progText = sendValue.endsWith('ep') ? 'Climate is Demand Response Prep' : 'Climate is Demand Response'
						} else if (sendValue == 'Offline') {
							progText = 'Thermostat is Offline'
						} else {
							progText = 'Climate is '+sendValue.trim().replaceAll(":","")
						}
						if (isChange || forceChange) {
							event = eventFront + [value: sendValue, descriptionText: progText, displayed: true]
						}
						break;

					case 'currentProgram':
						// LOG("currentProgram: ${sendValue}", 3, null, 'info')
						if (isChange) {
							event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
						}
						break;

					case 'apiConnected':
						// only display in the devices' log if we are in debug level 4 or 5
						if (isChange || forceChange) event = eventFront + [value: sendValue, descriptionText: "API Connection is ${value}", isStateChange: true, displayed: debugLevelFour]
						break;

					case 'weatherSymbol':
						Integer symbolNum = value as Integer
						String timeOfDay = device.currentValue('timeOfDay', true)
						if ((timeOfDay == 'night') && (symbolNum < 100)) {
							symbolNum = symbolNum + 100
						} else if ((timeOfDay == 'day') && (symbolNum >= 100)) {
							symbolNum = symbolNum - 100
						}
						isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
						if (isChange) event = [name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true]
						break;

					case 'timeOfDay':
						// Check to see if it is night time, if so change to a night symbol
						if (isChange) {
							def weatherSymbol = device.currentValue('weatherSymbol', true)
							Integer symbolNum = weatherSymbol as Integer
							if ((sendVal == 'night') && (symbolNum < 100)) {
								symbolNum = symbolNum + 100
							} else if ((sendVal == 'day') && (symbolNum >= 100)) {
								symbolNum = symbolNum - 100
							}
							isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
							if (isChange) sendEvent(name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true)
							event = eventFront + [value: sendValue, descriptionText: '', isStateChange: true, displayed: false]
						}
						break;

					case 'motion':

						if (isChange) {
							String cMotion = device.currentValue('motion', true)
							// Once "in/active", prevent inadvertent "not supported" -
							if (!cMotion || (cMotion == 'null') || (cMotion == "") || !sendValue.startsWith('not') || !cMotion.contains('act')) {
								event = eventFront + [value: sendValue, descriptionText: "Motion is "+sendValue, isStateChange: true, displayed: true]
							}
						}
						String occupancy = ((sendValue == 'active') ? 'occupied' : 'vacant')
						isChange = isChange ?: isStateChange(device, 'occupancy', occupancy)
						if (isChange) {
							sendEvent (name: 'occupancy', linkText: linkText, value: occupancy, descriptionText: "Occupancy is ${occupancy}", isStateChange: true, displayed: true)
							objectsUpdated++
						}
						break;

					case 'fanMinOnTime':
						if (isChange || forceChange) {
							String circulateText = (value == 0) ? 'Fan Circulation is disabled' : "Fan Circulation is ${sendValue} minutes per hour"
							event = eventFront + [value: sendValue, descriptionText: circulateText, /* isStateChange: isChange, */ displayed: true]
							String fanMode = device.currentValue('thermostatFanMode', true)
							if (fanMode != 'on') {
								if (tMode == 'off') {
									fanMode = (value == 0) ? 'off' : 'circulate'
								} else {
									fanMode = (value == 0) ? 'auto' : 'circulate'
								}
								if (event != [:]) { sendEvent(event); event = [:] }		// update fanMinOnTime attribute of the device
								sendEvent(name: 'thermostatFanMode', value: fanMode, displayed: false /*, isStateChange: true */)
							}
						}
						break;
					
					case 'fanSpeed':
                        String fso = device.currentValue('fanSpeedOptions',true)
                        List fanSpeedOptions = []
                        if (fso) fanSpeedOptions = new JsonSlurper().parseText(fso)
						String realFanSpeed = (fanSpeedOptions.size() <= 1) ? 'low' : 'optimized'
						if (fanSpeedOptions?.contains(sendValue) && (sendValue != 'medium')) {
							realFanSpeed = sendValue as String
						} else if ((sendValue == 'medium') && !fanSpeedOptions?.contains('medium') &&  fanSpeedOptions?.contains("high")) {				// work around for Ecobee idiosyncracy	 
							realFanSpeed = 'high' 					// medium turns on high if only 2 speeds exist
						}
                        String fs = device.currentValue('fanSpeed', true)
						isChange = (!fs || (fs != realFanSpeed))
						if (isChange || forceChange) event = eventFront + [value: realFanSpeed, descriptionText: "Fan Speed is ${realFanSpeed}", isStateChange: true, displayed: true]
						break;

					case 'thermostatMode':
						//log.debug "thermostatMode: ${sendValue}, isChange: ${isChange}, tMode: ${tMode}"
						tMode = sendValue 
						if (isChange || forceChange) {
							//log.debug "sendEvent thermostatMode"
							event = eventFront + [value: sendValue, descriptionText: "Thermostat Mode is ${sendValue}", data:[statModes: supportedThermostatModes], displayed: true]
						}
						switch (sendValue) {
							case 'off':
								if (isChange) {
									sendEvent(name: 'thermostatOperatingState', value: 'idle', displayed: true, descriptionText: 'Thermostat is idle')
									if (true || isIOS) {
										// Android doesn't handle 'off' for operatingState
										sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', displayed: false, isStateChange: true)
									}
								}
								break;

							case 'auto':
								// We need to update the thermostatSetpoint (for Google Home), so we send based upon lastRunningMode (from thermostatOperatingState, not thermostatMode).
								def newSetpoint
								lRunMode =  getDataValue("lastRunningMode") 
								if (lRunMode == 'heat') {
									newSetpoint = device.currentValue('heatingSetpoint', true)?.toString()
								} else if (lRunMode == 'cool') {
									newSetpoint = device.currentValue('coolingSetpoint', true)?.toString()
								} else {
                                	def hsp = device.currentValue('heatingSetpoint', true)
                                    def csp = device.currentValue('coolingSetpoint', true)
									newSetpoint = ((hsp && (hsp != 'null') && (hsp != "")) && (csp && (csp != 'null') && (csp != ""))) ? (roundIt(((hsp + csp) / 2.0), precision.toInteger())) : 'null'
								}
								objectsUpdated++
								break;

							case 'heat':
								String statSetpoint = device.currentValue('heatingSetpoint', true).toString()
								sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
								sendEvent(name: 'lastRunningMode', value: 'heat', displayed: false)
								updateDataValue('lastRunningMode', 'heat')
								state.lastRunningMode = 'heat'
								objectsUpdated++
								break;

							case 'cool':
								String statSetpoint = device.currentValue('coolingSetpoint', true).toString()
								sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
								sendEvent(name: 'lastRunningMode', value: 'cool', displayed: false)
								updateDataValue('lastRunningMode', 'cool')
								state.lastRunningMode = 'cool'
								objectsUpdated++
								break;

							case 'auxHeatOnly':
							case 'emergencyHeat':
								String statSetpoint = device.currentValue('heatingSetpoint', true).toString()
								sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
								sendEvent(name: 'lastRunningMode', value: 'heat', displayed: false)
								updateDataValue('lastRunningMode', 'heat')
								state.lastRunningMode = 'heat'
								objectsUpdated++
								break;
						}
						break;

					case 'thermostatFanMode':
						if (isChange || forceChange){
							event = eventFront + [value: sendValue, descriptionText: "Fan Mode is ${sendValue}", data:[supportedThermostatFanModes: fanModes()], displayed: true]
							sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false)
						}
						String thermostatHold = device.currentValue('thermostatHold', true)
						if (thermostatHold != 'vacation') {
							switch(sendValue) {
								case 'off':
									// Assume (for now) that thermostatMode is also 'off' - this should be enforced by setThermostatFanMode() (who is also only one	 who will send us 'off')
									break;

								case 'auto':
								case 'circulate':
									def fanMinOnTime = device.currentValue('fanMinOnTime', true)
									if (tMode == 'off') {
										if (fanMinOnTime != 0) {
											sendValue = 'circulate dis'
										} else {
											sendValue = 'off dis'		// display 'off' when Fan Mode == 'auto' && Thermostat Mode == 'off'
										}
									} else {
										if (fanMinOnTime != 0) { sendValue = 'circulate' } else { sendValue = 'auto' }
									}
									break;

								case 'on':
									break;
							}
						} 
						break;

					case 'debugEventFromParent':
					case 'appdebug':
						event = eventFront + [value: sendValue, descriptionText: "-> ${sendValue}", isStateChange: true, displayed: true]
						int ix = sendValue.lastIndexOf(" ")
						String msg = sendValue.substring(0, ix)
						String type = sendValue.substring(ix + 1).replaceAll("[()]", "")
						switch (type) {
							case 'error':
								log.error(msg)
								break;
							case 'trace':
								log.trace(msg)
								break;
							case 'info':
								log.info(msg)
								break;
							case 'warn':
								log.warn(msg)
								break;
							default:
								log.debug(msg)
						}
						break;

					case 'thermostatTime':
						// 2017-03-22 15:06:14
						if (isChange) {
							event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
						}
						break;

					// Update the new (Optional) SetpointMin/Max attributes
					case 'heatRangeLow':
						def ncHspmn = device.currentValue('heatingSetpointMin', true)
						if (isChange || forceChange || !ncHspmn || (ncHspmn != value)) {
							sendEvent(name: 'heatingSetpointMin', value: value, unit: tu, displayed: false)
							objectsUpdated++
							if ((tMode == 'heat') || (tMode == 'auto')) {
								sendEvent(name: 'thermostatSetpointMin', value: value, unit: tu, displayed: false)
								def ncTspmx = device.currentValue('thermostatSetpointMax', true)
								def range = [value, ncTspmx]
								sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
								def ncHspmx = device.currentValue('heatingSetpointMax', true)
								range = [value, ncHspmx]
								sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
								objectsUpdated += 3
							}
							event = eventFront + [value: value, unit: tu, displayed: false]
						}
						break;

					case 'heatRangeHigh':
						def ncHspmx = device.currentValue('heatingSetpointMax', true)
						if (isChange || forceChange || !ncHspmx || (ncHspmx != value)) {
							sendEvent(name: 'heatingSetpointMax', value: value, unit: tu, displayed: false)
							objectsUpdated++
							if (tMode == 'heat') {
								sendEvent(name: 'thermostatSetpointMax', value: value, unit: tu, displayed: false)
								def ncTspmn = device.currentValue('thermostatSetpointMin', true)
								def range = [ncTspmn, value]
								sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
								def ncHspmn = device.currentValue('heatingSetpointMin', true)
								range = [ncHspmn, value]
								sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
								objectsUpdated += 3
							}
							event = eventFront + [value: value, unit: tu, displayed: false]
						}
						break;

					case 'coolRangeLow':
						def ncCspmn = device.currentValue('coolingSetpointMin', true)
						if (isChange || forceChange || (ncCspmn!= value)) {
							sendEvent(name: 'coolingSetpointMin', value: value, unit: tu, displayed: false)
							objectsUpdated++
							if (tMode == 'cool') {
								sendEvent(name: 'thermostatSetpointMin', value: value, unit: tu, displayed: false)
								def ncTspmx = device.currentValue('thermostatSetpointMax', true)
								def range = [value, ncTspmx]
								sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
								def ncCspmx = device.currentValue('coolingSetpointMax', true)
								range = [value, ncCspmx]
								sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
								objectsUpdated += 3
							}
							event = eventFront + [value: value, unit: tu, displayed: false]
						}
						break;

					case 'coolRangeHigh':
						def ncCspmx = device.currentValue('coolingSetpointMax', true)
						if (isChange || forceChange || (ncCspmx != value)) {
							sendEvent(name: 'coolingSetpointMax', value: value, unit: tu, displayed: false)
							objectsUpdated++
							if ((tMode == 'cool') || (tMode == 'auto')) {
								sendEvent(name: 'thermostatSetpointMax', value: value, unit: tu, displayed: false)
								def ncTspmn = device.currentValue('thermostatSetpointMin', true)
								def range = [ncTspmn, value]
								sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
								def ncCspmn = device.currentValue('coolingSetpointMin', true)
								range = [ncCspmn, value]
								sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
								objectsUpdated += 3
							}
							event = eventFront + [value: value, unit: tu, displayed: false]
						}
						break;

					case 'heatRange':
						if (isChange || forceChange) {
							def newValue = sendValue.toString().replaceAll("[()]","")
							def idx = newValue.indexOf('..')
							def low = newValue.take(idx)
							def high = newValue.drop(idx+2)
							def range = [low,high]
							sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
							event = eventFront + [value: sendValue, unit: tu, displayed: false]
							objectsUpdated++
						}
						break;

					case 'coolRange':
						if (isChange || forceChange) {
							def newValue = sendValue.toString().replaceAll("[()]","")
							def idx = newValue.indexOf('..')
							def low = newValue.take(idx)
							def high = newValue.drop(idx+2)
							def range = [low,high]
							sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
							event = eventFront + [value: sendValue, unit: tu, displayed: false]
							objectsUpdated++
						}
						break;

					case 'ecobeeConnected':
						if (isChange) {
							event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
						}
						break;

					case 'thermostatHold':
                        String descText
                        String sendText = (sendValue && (sendValue != 'null') && (sendValue != '')) ? sendValue : 'null'
                        if (sendText == 'vacation') {
                            descText = "Vacation hold started"
                        } else if (sendText == 'null') {
                            def thermostatHold = device.currentValue('thermostatHold', true)
                            descText = 'Hold finished'
                        } else if (sendText == 'hold') {
                            String ncCp = device.currentValue('currentProgram', true)
                            String ncSp = device.currentValue('scheduledProgram', true)
                            descText = "Hold for ${ncCp} (${ncSp})" 
                        } else descText = "Hold for ${sendText}"
                        event = eventFront + [value: sendText, descriptionText: descText, isStateChange: true, displayed: true]
						break;
                        
					case 'holdStatus':
                    case 'thermostatStatus':
						String sendText = (sendValue && sendValue != 'null' && (sendValue != "")) ? sendValue : " "
						event = eventFront + [value: sendText, descriptionText: sendText, displayed: (sendText != " ")]
						break;
                        
					case 'holdEndDate':
						String sendText = (sendValue && (sendValue != "") && (sendValue != 'null')) ? sendValue : 'null'
                        String descText = (sendText != 'null') ? "Hold ends ${sendText}" : ""
						event = eventFront + [value: sendText, descriptionText: descText, displayed: true]
						break;
                        
                    case 'schedText':
                    	String sendText = (sendValue && (sendValue != "") && (sendValue != 'null')) ? sendValue : 'null'
						event = eventFront + [value: sendText, descriptionText: "", displayed: false]
                        break;
                        
                    case 'schedule':
						if (isChange) {
                        	event = eventFront + [value: sendValue, descriptionText: "Schedule is ${sendValue}", isStateChange: true, displayed: true]
						}
                        break;
                        
					case 'holdEndsAt':                    
						String sendText = (sendValue && (sendValue != 'null') && (sendValue != "")) ? sendValue : 'null'
						event = eventFront + [value: sendText, descriptionText: "", displayed: false]
						break;


					case 'hasDehumidifier':
						if (sendValue == 'false') {
							sendEvent(name: 'dehumidifierMode', value: 'off', displayed: false)
						}
						if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
						break;

					case 'hasHumidifier':
						if (sendValue == 'false') {
							sendEvent(name: 'humidifierMode', value: 'off', displayed: false)
						}
						if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
						break;

					case 'lastRunningMode':
						if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
						updateDataValue("lastRunningMode", sendValue)	// For Google Home - wants it in a data variable
						break;

					case 'scheduleUpdated':
						event = eventFront + [value: sendValue, isStateChange: true, displayed: false, descriptionText: "Thermostat Schedule last updated: ${sendValue}"]
						break;
                        
       				case 'voiceEngines':
                    	event = eventFront + [value: ((!sendValue || (sendValue == 'null') || (sendValue == "")) ? [] : sendValue), descriptionText: "", displayed: false]
                        break;                   

					// The following are all string values that can be 'null' - send "" instead
					case 'eiLocation':
					case 'features':
					case 'groupName':
					case 'groupRef':
					case 'groupSetting':
					case 'name':
                    case 'mobile':
					case 'userAccessCode':
					case 'ventilatorOffDateTime':
					case 'playbackVolume':
					case 'microphoneEnabled':
					case 'soundAlertVolume':
					case 'soundTickVolume':
						if (sendValue && (sendValue != 'null') && (sendValue != "")) {
							event = eventFront + [value: sendValue, descriptionText: "${name} is ${value}", displayed: true]
						} else {
							event = eventFront + [value: 'null', descriptionText: "", isStateChange: true, displayed: false]
						}
						break;

					// The following are all temperature values, send with the appropriate temperature unit (tu)
					case 'heatDifferential':
					case 'coolDifferential':
					case 'heatCoolMinDelta':
					case 'stage1CoolingDifferentialTemp':
					case 'stage1HeatingDifferentialTemp':
					case 'dehumidifyOvercoolOffset':
					case 'compressorProtectionMinTemp':
					case 'auxMaxOutdoorTemp':
					case 'maxSetBack':
					case 'maxSetForward':
					case 'quickSaveSetBack':
					case 'quickSaveSetForward':
					case 'coolMaxTemp':
					case 'coolMinTemp':
					case 'heatMaxTemp':
					case 'heatMinTemp':
					case 'tempCorrection':
						if (isChange) event = eventFront +	[value: sendValue, unit: tu, isStateChange: true, displayed: false]
						break;

					case 'vent':
						if (isChange) {
							sendEvent(name: 'ventMode', value: sendValue, isStateChange: true, displayed: false)
							event = eventFront +  [value: sendValue, descriptionText: "Vent is ${sendValue}", isStateChange: true, displayed: true]
							// log.debug "vent: ${sendValue}"
						}
						break;
                        
                    case 'debugLevel':
                        String sendText = (sendValue && (sendValue != 'null') && (sendValue != "")) ? sendValue : 'null'
                        updateDataValue('debugLevel', sendText)
						event = eventFront + [value: sendText, descriptionText: "debugLevel is ${sendValue}", isStateChange: true, displayed: false]
                        break;

					// These are ones we don't need to display or provide descriptionText for (mostly internal or debug use)
					case 'autoMode':
						// we piggyback the long name on the short name 'autoMode'
						if (isChange) sendEvent(name: 'autoHeatCoolFeatureEnabled', value: sendValue, isStateChange: true, displayed: false)
					case 'heatMode':
					case 'coolMode':
					case 'auxHeatMode':
						String modeValue = (name == "auxHeatMode") ? "auxHeatOnly" : name - "Mode"
						if (sendValue == 'true') {
							if (!supportedThermostatModes.contains(modeValue)) {
								supportedThermostatModes += [modeValue]
							}
						} else {
							if (supportedThermostatModes.contains(modeValue)) {
								supportedThermostatModes -= [modeValue]
							}
						}
						if (state.supportedThermostatModes != supportedThermostatModes) {
							state.supportedThermostatModes = supportedThermostatModes.sort(false)
							String modesJSON = new groovy.json.JsonBuilder(state.supportedThermostatModes).toString()
							sendEvent(name: "supportedThermostatModes", value: modesJSON, displayed: false, isStateChange: true)
							sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false, isStateChange: true)
							supportedThermostatModes = state.supportedThermostatModes
						}
					// Removed all the miscellaneous settings - they will thus appear in the device event list via 'default:' (May 10, 2019 - BAB)
					// This *should* improve the efficiency of the 'switch', at least a little
					case 'decimalPrecision':
					case 'currentProgramId':
					case 'currentProgramOwner':
					case 'currentProgramType':
					case 'scheduledProgramName':
					case 'scheduledProgramId':
					case 'scheduledProgram':
					case 'scheduledProgramOwner':
					case 'scheduledProgramType':
					case 'programsList':
					case 'climatesList':
					case 'temperatureScale':
					case 'checkInterval':
					case 'statHoldAction':
					case 'lastHoldType':
					case 'thermostatOperatingStateDisplay':
					case 'nextProgramName':
					case 'nextHeatingSetpoint':
					case 'nextCoolingSetpoint':
						String sendText = (sendValue && (sendValue != 'null') && (sendValue != "")) ? sendValue : 'null'
						String descText = (sendText != 'null') ? "${name} is ${sendText}" : ""
						event = eventFront + [value: sendText, descriptionText: descText, /* isStateChange: true, */ displayed: false]
						break;

					// everything else gets displayed once with generic text
					default:
						if (isChange) {
							if (name.endsWith("Updated")) {		// internal timestamps for updates
								event = eventFront + [value: sendValue, descriptionText: "${name} at ${sendValue}", isStateChange: true, displayed: false]
							} else {
								event = eventFront + [value: sendValue, descriptionText: "${name} is ${sendValue}", isStateChange: true, displayed: true]
							}
						}
						break;
				}
				if (event != [:]) {
					if (debugLevelFour) LOG("generateEvent() - Out of switch{}, calling sendevent(${event})", 1)
					sendEvent(event)
				}
			}

			//generateSetpointEvent()
			//generateStatusEvent()
		}
        // Clear any lingering thermostatStatus strings.
        String statStatus = device.currentValue('thermostatStatus', true)
        if (statStatus && (statStatus.trim() != "")) sendEvent( name: 'thermostatStatus', value: " ", displayed: false, isStateChange: true)
	}
	def elapsed = now() - startMS
	LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)", 2, this, 'info')
	// if (elapsed > 2500) log.debug updates
}

//return descriptionText to be shown on mobile activity feed
String getTemperatureDescriptionText(name, value, linkText) {
	switch (name) {
		case 'temperature':
			return "Temperature is ${value}°${temperatureScale}"
			break;
		case 'heatingSetpoint':
			return "Heating setpoint is ${value}°${temperatureScale}"
			break;
		case 'coolingSetpoint':
			return "Cooling setpoint is ${value}°${temperatureScale}"
			break;
		case 'thermostatSetpoint':
			return "Thermostat setpoint is ${value}°${temperatureScale}"
			break;
		case 'weatherTemperature':
			return "Outdoor temperature is ${value}°${temperatureScale}"
			break;
		case 'weatherDewpoint':
			return "Outdoor dew point is ${value}°${temperatureScale}"
	}
	return ""
}

// ***************************************************************************
// Thermostat setpoint commands
// API calls and UI handling
// ***************************************************************************
void setHeatingSetpointDelay(setpoint) {
	LOG("Slider requested heat setpoint: ${setpoint}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause')?: 4) as Integer
	runIn( runWhen, 'sHS', [data: [setpoint:setpoint.toBigDecimal()]] )
}
void sHS(data) {
	LOG("Setting heating setpoint to: ${data.setpoint}",4,null,'trace')
	setHeatingSetpoint(data.setpoint.toBigDecimal())
}
void setHeatingSetpoint(setpoint, String sendHold=null) {
	
	def thermostatHold = device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("setHeatingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
    if (!setpoint || (setpoint == '') || (setpoint == 'null')) {
    	LOG("setHeatingSetpoint(${setpoint}) - Invalid setpoint",2,null,'warn')
		return
    }

	boolean isMetric = wantMetric()
	//def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')

	if (!isMetric && (setpoint < 35.0)) {
		setpoint = roundIt(cToF(setpoint), 1)	// Hello, Google hack - seems to request C when stat is in Auto mode
		LOG ("setHeatingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
	}

	def heatingSetpoint = isMetric ? roundC(setpoint) : roundIt(setpoint, 1)

	//enforce limits of heatingSetpoint range
	LOG("setHeatingSetpoint() before adjustment: ${heatingSetpoint}°${temperatureScale}", 4, null, 'trace')
	def low  = device.currentValue('heatRangeLow', true).toBigDecimal()		// already converted to C by Parent
	def high = device.currentValue('heatRangeHigh', true).toBigDecimal()
	LOG("setHeatingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
	// Silently adjust the temps to fit within the specified ranges
	if (heatingSetpoint < low ) { heatingSetpoint = isMetric ? roundC(low) : low }
	else if (heatingSetpoint > high) { heatingSetpoint = isMetric ? roundC(high) : high }

	LOG("setHeatingSetpoint() requesting heatingSetpoint: ${heatingSetpoint}°${temperatureScale}", 2, null, 'info')
	state.newHeatingSetpoint = heatingSetpoint
	state.useThisHold = sendHold
	runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

void setCoolingSetpointDelay(setpoint) {
	LOG("Slider requested cool setpoint: ${setpoint}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause') ?: 4 ) as Integer
	runIn( runWhen, 'sCS', [data: [setpoint:setpoint.toBigDecimal()]] )
}
void sCS(data) {
	LOG("Setting cooling setpoint to: ${data.setpoint}",4,null,'trace')
	setCoolingSetpoint(data.setpoint.toBigDecimal())
}
void setCoolingSetpoint(setpoint, String sendHold='') {
	
	def thermostatHold = device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("setCoolingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
    if (!setpoint || (setpoint == '') || (setpoint == 'null')) {
    	LOG("setCoolingSetpoint(${setpoint}) - Invalid setpoint",2,null,'warn')
        return
    }
	boolean isMetric = wantMetric()
	//def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')

	if (!isMetric && (setpoint < 35.0)) {
		setpoint = roundIt(cToF(setpoint), 1)	// Hello, Google hack - seems to request C when stat is in Auto mode
		LOG ("setCoolingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
	}

	def coolingSetpoint = isMetric ? roundC(setpoint) : roundIt(setpoint, 1)

	//enforce limits of heatingSetpoint vs coolingSetpoint
	LOG("setCoolingSetpoint() before adjustment: coolingSetpoint = ${coolingSetpoint}°${temperatureScale}", 4, null, 'trace')
	def low  = device.currentValue('coolRangeLow', true).toBigDecimal()
	def high = device.currentValue('coolRangeHigh', true).toBigDecimal()
	LOG("setCoolingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
	// Silently adjust the temps to fit within the specified ranges
	if (coolingSetpoint < low ) { coolingSetpoint = isMetric ? roundC(low) : low }
	else if (coolingSetpoint > high) { coolingSetpoint = isMetric ? roundC(high) : high }

	LOG("setCoolingSetpoint() requesting coolingSetpoint: ${coolingSetpoint}°${temperatureScale}", 2, null, 'info')
	state.newCoolingSetpoint = coolingSetpoint.toBigDecimal()
	state.useThisHold = sendHold
	runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

void updateThermostatSetpoints() {
	boolean isMetric = wantMetric()
	def deviceId = getDeviceId()

	def heatingSetpoint = state.newHeatingSetpoint ? state.newHeatingSetpoint : (device.currentValue('heatingSetpoint', true)).toBigDecimal()
	def coolingSetpoint = state.newCoolingSetpoint ? state.newCoolingSetpoint : (device.currentValue('coolingSetpoint', true)).toBigDecimal()
	LOG("updateThermostatSetpoints(): heatingSetpoint ${heatingSetpoint}°, coolingSetpoint ${coolingSetpoint}°", 2, null, 'info')

	def sendHoldType = state.useThisHold
	if (sendHoldType) { state.useThisHold = null } else { sendHoldType = whatHoldType() }
	def sendHoldHours = null
	if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
		sendHoldHours = sendHoldType
		sendHoldType = 'holdHours'
	}
	LOG("updateThermostatSetpoints(): sendHoldType == ${sendHoldType} ${sendHoldHours}", 4, null, 'trace')

	// IFF we have Auto mode enabled, make sure we maintain the proper heatCoolMinDelta
    def hasAutoMode = ((device.currentValue('autoMode', true)) == 'true') ? true : false
	def delta = (device.currentValue('heatCoolMinDelta', true))?.toBigDecimal()
    if (!hasAutoMode || !delta) delta = 0.0
	//if (!delta) delta = isMetric ? 0.5 : 1.0
    
    if (hasAutoMode) {
        if ((heatingSetpoint + delta) > coolingSetpoint) {
            // we have to adjust heat/cool
            if (state.newHeatingSetpoint) {
                if (!state.newCoolingSetpoint) {
                    // got request to change heat only, push cool up
                    coolingSetpoint = heatingSetpoint + delta
                } else {
                    // got request to change both, let's see what the thermostat is doing
                    def mode = device.currentValue('thermostatMode', true)
                    if (mode == 'heat') {
                        // we are in Heat Mode, so raise the cooling setpoint
                        coolingSetpoint = heatingSetpoint + delta
                    } else if (mode == 'cool') {
                        // we are in Cool Mode, so lower the heating setpoint
                        heatingSetpoint = coolingSetpoint - delta
                    } else if (mode == 'auto') {
                        // Auto Mode - let's see if the heat or cooling is on
                        def opState = device.currentValue('thermostatOperatingState', true)
                        if (opState.contains('ea')) {
                            // currently heating, raise the cooling setpoint
                            coolingSetpoint = heatingSetpoint + delta
                        } else if (opState.contains('oo')) {
                            // currently cooling, lower the heating setpoint
                            heatingSetpoint = coolingSetpoint - delta
                        } else {
                            // Auto & Idle, check what our last operating state was and adjust the opposite
                            def ncLos = device.currentValue('lastOpState', true)
                            if (ncLos?.contains('ea')) {
                                // We were last heating, raise the cooling setpoint
                                coolingSetpoint = heatingSetpoint + delta
                            } else {
                                // Must have been cooling, lower the heating setpoint
                                heatingSetpoint = coolingSetpoint - delta
                            }
                        }
                    }
                }
            } else if (state.newCoolingSetpoint) {
                // heat not requested, so just lower the heating setpoint
                heatingSetpoint = coolingSetpoint - delta
            }
            LOG("updateThermostatSetpoints() adjusted setpoints, heat ${heatingSetpoint}°, cool ${coolingSetpoint}°", 2, null, 'info')
        }
    }

	if (parent.setHold(this, heatingSetpoint,  coolingSetpoint, deviceId, sendHoldType, sendHoldHours)) {
		def updates = [ coolingSetpoint: roundIt(coolingSetpoint, 1),	// was Display
						heatingSetpoint: roundIt(heatingSetpoint, 1),	// was Display
						lastHoldType: sendHoldType,
                        thermostatHold: 'hold']
		generateEvent(updates)
		def thermostatSetpoint = device.currentValue('thermostatSetpoint', true)
		LOG("Done updateThermostatSetpoints() coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}, thermostatSetpoint: ${thermostatSetpoint}",4,null,'trace')
		// generateStatusEvent()
	} else {
		LOG("Error updateThermostatSetpoints()", 2, null, "error") //This error is handled by the connect app (huh???)
	}
	state.newHeatingSetpoint = null
	state.newCoolingSetpoint = null
	runIn(5, 'refresh', [overwrite: true])
}

// raiseSetpoint: called by tile when user hit raise temperature button on UI
void raiseSetpoint() {
	LOG('raiseSetpoint()',4,null,'trace')
	
	// Cannot change setpoint while in Vacation mode
	def thermostatHold = device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}

	String mode = device.currentValue("thermostatMode", true)
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("raiseSetpoint(): this mode: (${mode}) does not support raiseSetpoint${mode=='auto'?' - try enabling Smart Auto in Ecobee Suite Manager preferences':''}",1,null,'warn')
		return
	}

	boolean isMetric = wantMetric()
	boolean changingHeat = true			// is a change already in process?
	boolean changingCool = true
	def heatingSetpoint = state.newHeatSetpoint // device.currentValue('newHeatSetpoint')?.toDouble()
	if (!heatingSetpoint || (heatingSetpoint == 0.0)) {
		heatingSetpoint = (device.currentValue("heatingSetpoint", true))?.toBigDecimal()
		changingHeat = false
	}
	def coolingSetpoint = state.newCoolSetpoint /// device.currentValue('newCoolSetpoint')?.toDouble()
	if (!coolingSetpoint || (coolingSetpoint == 0.0)) {
		coolingSetpoint = (device.currentValue("coolingSetpoint", true))?.toBigDecimal()
		changingCool = false
	}
	def thermostatSetpoint = (device.currentValue("thermostatSetpoint", true))?.toBigDecimal()
	def operatingState = device.currentValue("thermostatOperatingState", true)
	LOG("raiseSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')

	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def targetValue
	boolean smartAuto = usingSmartAuto()
	def runWhen = (getParentSetting('arrowPause') ?: 4 ) as Integer
	if ((mode == 'auto') && smartAuto) {
		raiseSmartSetpoint(heatingSetpoint, coolingSetpoint)
		return
	}
    def ncLos = device.currentValue('lastOpState', true)
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || ncLos?.contains('ea')))) {
		targetValue = roundIt((heatingSetpoint + (isMetric ? 0.5 : 1.0)),1)
		LOG("raiseSetpoint() Heating to ${targetValue}", 4)
		state.newHeatSetpoint = targetValue		// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: targetValue]]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
	} else if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAudio && (operatingState.contains('oo') || ncLos?.contains('oo')))) {
		targetValue = roundIt((coolingSetpoint + (isMetric ? 0.5 : 1.0)), 1)
		LOG("raiseSetpoint() Cooling to ${targetValue}", 4)
		state.newCoolSetpoint = targetValue		// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: targetValue]]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
	}
}

// lowerSetpoint: called by tile when user hit lower temperature button on UI
void lowerSetpoint() {
	LOG('lowerSetpoint()',4,null,'trace')
	
	// Cannot change setpoint while in Vacation mode
	def thermostatHold = device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	
	String mode = device.currentValue("thermostatMode", true)
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("lowerSetpoint(): this mode: $mode does not support lowerSetpoint${mode=='auto'?' - try enabling Smart Auto in Ecobee Suite Manager preferences':''}", 1, null, "warn")
		return
	}

	boolean isMetric = wantMetric()
	boolean changingHeat = true			// is a change already in process?
	boolean changingCool = true
	def heatingSetpoint = state.newHeatSetpoint		// device.currentValue('newHeatSetpoint')?.toDouble()
	if (!heatingSetpoint || (heatingSetpoint == 0.0)) {
		heatingSetpoint = device.currentValue("heatingSetpoint", true)?.toBigDecimal()
		changingHeat = false
	}
	def coolingSetpoint = state.newCoolSetpoint		// device.currentValue('newCoolSetpoint')?.toDouble()
	if (!coolingSetpoint || (coolingSetpoint == 0.0)) {
		coolingSetpoint = device.currentValue("coolingSetpoint", true)?.toBigDecimal()
		changingCool = false
	}
	def thermostatSetpoint = device.currentValue("thermostatSetpoint", true).toBigDecimal()
	def operatingState = device.currentValue("thermostatOperatingState", true)
	LOG("lowerSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')

	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def targetValue
	boolean smartAuto = usingSmartAuto()
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer

	if ((mode == 'auto') && smartAuto) {
		lowerSmartSetpoint(heatingSetpoint, coolingSetpoint)
		return
	}
	def ncLos = device.currentValue('lastOpState', true)
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || ncLos?.contains('ea')))) {
		targetValue = roundIt((heatingSetpoint - (isMetric ? 0.5 : 1.0)), 1)
		LOG("lowerSetpoint() Heating to ${targetValue}", 4)
		state.newHeatSetpoint = targetValue			// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: targetValue]]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
	}
	if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAuto && (operatingState.contains('oo') || ncLos?.contains('oo')))) {
		targetValue = roundIt((coolingSetpoint - (isMetric ? 0.5 : 1.0)), 1)
		LOG("lowerSetpoint() Cooling to ${targetValue}", 4)
		state.newCoolSetpoint = targetValue			// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: targetValue]]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
	}
}

// changeHeatSetpoint: effect setHeatingSetpoint for UI
void changeHeatSetpoint(newSetpoint) {
	def updates = [[heatingSetpoint: newSetpoint.value.toBigDecimal()], [currentProgramName:'Hold: Temp']]
	generateEvent(updates)
	LOG("changeHeatSetpoint(${newSetpoint.value}°)",2,null,'trace')
	setHeatingSetpoint( newSetpoint.value.toBigDecimal() )
	state.newHeatSetpoint = null	// sendEvent(name: 'newHeatSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// changeCoolSetpoint: effect setCoolingSetpoint for UI
void changeCoolSetpoint(newSetpoint) {
	def updates = [[coolingSetpoint: newSetpoint.value.toBigDecimal()], [currentProgramName:'Hold: Temp']]
	generateEvent(updates)
	LOG("changeCoolSetpoint(${newSetpoint.value}°)",2,null,'trace')
	setCoolingSetpoint( newSetpoint.value.toBigDecimal() )
	state.newCoolSetpoint = null	// sendEvent(name: 'newCoolSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// raiseSmartSetpoint: figure out which setpoint to raise
void raiseSmartSetpoint(heatingSetpoint, coolingSetpoint) {
	boolean isMetric = wantMetric()
	def currentTemp = device.currentValue('temperature', true).toBigDecimal()
	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def adjust = (isMetric ? 0.5 : 1.0)
	def newHeat = roundIt((heatingSetpoint + adjust), 1)
	def newCool = roundIt((coolingSetpoint + adjust), 1)

	if (newHeat > currentTemp) {
		// turn the heat up
		LOG("raiseSmartSetpoint() Heating to ${newHeat}", 4)
		state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: newHeat]]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
	} else {
		// turn the cool up
		LOG("raiseSmartSetpoint() Cooling to ${newCool}", 4)
		state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: newCool]]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
	}
}

// lowerSmartSetpoint: figure out which setpoint to lower
void lowerSmartSetpoint(heatingSetpoint, coolingSetpoint) {
	boolean isMetric = wantMetric()
	def currentTemp = device.currentValue('temperature', true).toBigDecimal()
	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def adjust = (isMetric ? 0.5 : 1.0)
	def newHeat = roundIt((heatingSetpoint - adjust), 1)
	def newCool = roundIt((coolingSetpoint - adjust), 1)

	if (newCool < currentTemp) {
		// turn the cool down
		LOG("lowerSmartSetpoint() Cooling to ${newCool}", 4)
		state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: newCool]]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
	} else {
		// turn the heat down
		LOG("lowerSmartSetpoint() Heating to ${newHeat}", 4)
		state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
		def updates = [[thermostatSetpoint: newHeat]]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
	}
}

void resetUISetpoints() {
	state.newHeatSetpoint = null		// NOTE: different names than used by setHeatingSetpoint/setCoolingSetpoint is INTENTIONAL
	state.newCoolSetpoint = null		// in case some API calls to change temps while user is manually changing them also
}

// alterSetpoint: change setpoint (obsolete: no longer used as of v1.2.21)
void alterSetpoint(temp) {
	LOG('alterSetpoint called in error',1,null,'error')
}

void generateQuickEvent(String name, String value, Integer pollIn=0) {
	sendEvent(name: name, value: value, displayed: false)
	if (pollIn > 0) { runIn(pollIn, refresh, [overwrite: true]) }
}

// ***************************************************************************
// Thermostat Mode commands
// Note that the Thermostat Mode CAN be changed while in Vacation mode
// ***************************************************************************
void setThermostatMode(String value) {
	//	Uses Ecobee Modes: "auxHeatOnly" "heat" "cool" "off" "auto"
	value = value.trim()			// Hubitat Dashboard's Thermostat template adds extra space to fanMode, so just in case...
    
	if (value.startsWith('emergency')) { value = 'auxHeatOnly' }
	LOG("setThermostatMode(${value})", 5)

	List validModes = statModes()		// device.currentValue('supportedThermostatModes')
	if (!validModes.contains(value)) {
		LOG("Requested Thermostat Mode (${value}) is not supported by ${device.displayName}", 2, null, 'warn')
        if (value == 'auto') {
        	
        }
		return
	}

	boolean changed = true
    def tMode = device.currentValue('thermostatMode', true) 
	if (tMode != value) {
		if (parent.setMode(this, value, getDeviceId())) {
			// generateQuickEvent('thermostatMode', value, 5)
			def updates = [[thermostatMode:value]]
			if (value == 'off') {
				updates << [equipmentOperatingState:'off', thermostatOperatingState:'idle']
                def tFMode = device.currentValue('thermostatFanMode', true)
				if ( tFMode == 'auto') {
                	def ncFMOT = device.currentValue('fanMinOnTime', true)
					if ( ncFMOT == 0) {
						updates << [thermostatFanMode:'off',thermostatFanModeDisplay:'off']		// generateEvent will make this "fan dis"
					} else {
						updates << [thermostatFanMode:'circulate',thermostatFanModeDisplay:'circulate']
					}
				}
			} else if (tMode == 'off') {
				// Clean up when turning back on (heat/auto/cool)
				updates << [equipmentOperatingState:'idle',thermostatOperationsState:'idle',thermostatOperatingStateDisplay:'idle']
			}
			// log.debug "updates: ${updates}"
			generateEvent(updates)	// force everything to update
		} else {
			LOG("Failed to change Thermostat Mode to ${value}, Mode is ${device.currentValue('thermostatMode')}", 1, null, 'error')
			changed = false
		}
	}
	if (changed) LOG("Thermostat Mode changed to ${value}",2,null,'info')
}
List statModes() {
	return state.supportedThermostatModes as List
}
void off() {
	LOG('off()', 4, null, 'trace')
	setThermostatMode('off')
}
void heat() {
	LOG('heat()', 4, null, 'trace')
	setThermostatMode('heat')
}
void auxHeatOnly() {
	LOG("auxHeatOnly()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
void emergency() {
	LOG("emergency()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
// This is the proper definition for the capability
void emergencyHeat() {
	LOG("emergencyHeat()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
void cool() {
	LOG('cool()', 4, null, 'trace')
	setThermostatMode('cool')
}
void auto() {
	LOG('auto()', 4, null, 'trace')
	setThermostatMode('auto')
}

// ***************************************************************************
// Thermostat Program (aka Climates) Commands
// Program/Climates CANNOT be changed while in Vacation mode
// ***************************************************************************
void setThermostatHoldHours(holdHours=0) {
    String hours = holdHours.toInteger().toString()
	
    if (!hours || (hours == '0')) {
		device.updateSetting('myHoldHours', ""); settings.myHoldHours = null; state.defaultHoldHours = null
		LOG("setThermostatHoldHours(${holdHours}): setting default hold hours to ES Manager setting",2,null,'trace')
	} else {
		device.updateSetting('myHoldHours', hours); settings.myHoldHours = hours; state.defaultHoldHours = hours
		LOG("setThermostatHoldHours(${holdHours}): setting default hold hours to ${hours}",2,null,'trace')
	}   
	//log.debug "MyHoldHours = ${settings.myHoldHours}"
}
void setThermostatProgram(String program, String holdType="", Integer holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	refresh()
	
	String currentThermostatHold = device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setThermostatProgram(${program}): program change requested but ${device.displayName} is in Vacation mode, ignoring request",2,null,'warn')
		return
	}

	def programsList = []
	def programs = device.currentValue('programsList')
	if (!programs) {
		LOG("Supported programs list not initialized, possible installation error", 1, this, 'warn')
		programsList = ["Away","Home","Sleep","Resume"]		// Just use the default list
	} else {
		programsList = new JsonSlurper().parseText(programs) + ['Resume']
	}

	if ((program == null) || (program == "") || (!programsList.contains(program))) {
		LOG("setThermostatProgram( ${program} ): Missing or Invalid argument - must be one of (${programsList.toString()[1..-2]})", 2, this, 'warn')
		return
	}

	def deviceId = getDeviceId()
	LOG("setThermostatProgram(${program}, ${holdType}, ${holdHours})", 4, this,'trace')

	if (program == 'Resume') {
		LOG("setThermostatProgram() Resuming scheduled program", 2, this, 'info')
		resumeProgram( true )
		return
	}

	String sendHoldType = ""
	def sendHoldHours = null
	if (holdType && (holdType != "")) {
		sendHoldType = holdType
		if (holdType == 'holdHours') sendHoldHours = holdHours
	} else {
		sendHoldType =	whatHoldType()
		//log.debug "sendHoldType: ${sendHoldType}"
		if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
			sendHoldHours = sendHoldType
			sendHoldType = 'holdHours'
		}
	}
	// refresh()		// need to know if scheduled program changed recently

	String currentProgram 
	String currentProgramName
	String scheduledProgram 
   	currentProgram =	device.currentValue('currentProgram', true)
  	currentProgramName =device.currentValue('currentProgramName', true)
   	scheduledProgram =  device.currentValue('scheduledProgram', true)

	if (currentProgram == program) {
		if ((currentProgram == currentProgramName) && (sendHoldType == 'nextTransition')) {
			// Already running desired program, not in a hold, and Program will change at next transition as requested
			LOG("Thermostat Program is ${program} (already)", 2, this, 'info')
			return
		} else if ((currentProgramName == "Hold: ${currentProgram}") || (currentProgramName == "Auto ${currentProgram}")) {
			// We are already in a Hold for the requested program
			if ((scheduledProgram == program) && (sendHoldType == 'nextTransition')) {
				// The scheduled program is the same as the requested one, and caller wants next transition, so we can just resume that program
				// This really should not occur, but we essentially just want to pop the stack of Holds if it does
				LOG("Thermostat Program is ${program} (resumed)", 2, this, 'info')
				resumeProgramInternal(true)
				// runIn(5, refresh, [overwrite: false])
				generateProgramEvent(program,'')		// this may be redundant - resumeProgramInternal does this also
				return
			} else {
				// Otherwise (already in a hold for the desired program), do nothing if we can verify that the holdType is the same
				def lastHoldType = device.currentValue('lastHoldType', true)
				if (lastHoldType && (sendHoldType == lastHoldType) && (sendHoldType != 'holdHours')) {	// we have to reset the timer on an Hourly hold
					LOG("Thermostat Program is already Hold: ${program} (${sendHoldType})", 2, this, 'info')
					return
				}
			}
		}
	}

	// if the requested program is the same as the one that is supposed to be running, then just resumeProgram
	boolean needRefresh = true
	if (scheduledProgram == program) {
		if (resumeProgramInternal(true)) {							// resumeAll so that we get back to scheduled program
			LOG("Thermostat Program is ${program} (resumed)", 2, this, 'info')
			if (sendHoldType == 'nextTransition') {					// didn't want a permanent hold, so we are all done (don't need to set a new hold)
				// runIn(3, refresh, [overwrite: false])
				generateProgramEvent(program,'')
				def updates = [[ lastHoldType: sendHoldType ]]
				generateEvent(updates)
				return
			}
		}
	} else {
		if ((currentThermostatHold == 'hold') || (currentProgramName.startsWith('Auto')) || (currentProgramName.startsWith('Hold:'))) resumeProgramInternal(true)	// resumeAll before we change the program
		needRefresh = false
	}
	if ( parent.setProgram(this, program, deviceId, sendHoldType, sendHoldHours) ) {
		if (needRefresh) runIn(5, refresh, [overwrite: true])

		LOG("Thermostat Program is Hold: ${program}",2,this,'info')
		generateProgramEvent(program, program)				// ('Hold: '+program)
		def updates = [[lastHoldType: sendHoldType],
					   [thermostatHold: 'hold']]
		generateEvent(updates)
	} else {
		LOG("Error setting Program to ${program}", 2, this, "warn")
		// def priorProgram = device.currentState("currentProgramId")?.value
		// generateProgramEvent(priorProgram, program) // reset the tile back
	}
}

// SmartThings somewhere decided that "schedule" would be used to define "programs" ("climates" on Ecobee). But they punted on the argument for the call,
// using the ill-defined "scheduleJson" as a JSON_OBJECT. For this implementation, we'll allow quite a bit of latitude for what this is, including just a
// simple string.
void setSchedule(scheduleJson) {
	LOG("setSchedule( ${scheduleJson} )", 4, this, 'trace')
	if ((scheduleJson != null) && (scheduleJson != '[null]') && (scheduleJson != 'null') && (scheduleJson != '')) {
		if (scheduleJson instanceof CharSequence) {
			// Handle Strings made to look like JSON (e.g. commands from Hubitat Hubconnect device driver)
			while (scheduleJson.startsWith('[') || scheduleJson.startsWith('{')) { scheduleJson = scheduleJson[1..-2] }	// Recursively trim brackets/braces
			if (scheduleJson.contains(':')) {
				// Looks like a Map masquerading as a String
				def scheduleList = scheduleJson.contains(',') ? scheduleJson.split(',') : [scheduleJson]
				scheduleJson = [:]
				scheduleList.each {
					def m = it.split(':')
					scheduleJson += [ (m[0].trim()) : m[1].trim() ]
				}
			} else if (scheduleJson.contains(',')) {
				// Looks like a List masquerading as a String
				if (scheduleJson.contains('name')) {
					// It's actually a lazy Map (name, Home, holdType, nextTransition) masquerading as a String
					def scheduleList = scheduleJson.split(',')
					def s = scheduleList.size()
					int i
					scheduleJson = [:]
					for (i=0; i+1 <= s; i=i+2) {
						scheduleJson += [ (scheduleList[i].trim()) : scheduleList[i+1].trim() ]
					}
				} else {
					// OK, it's really just a List (Home, nextTransition) masquerading as a String
					scheduleJson = (scheduleJson.split(',') + [null, null]) as List			// Pad with extra arguments
				}
			} // else it's a one-argument String - leave it alone if not a Map or a List
		}
		LOG("scheduleJson  -  isMap: ${scheduleJson instanceof Map}, isList: ${scheduleJson instanceof List}, isCharSequence: ${scheduleJson instanceof CharSequence}", 4, this, 'debug')
		LOG("setSchedule( ${scheduleJson} )", 2, this, 'info')
		if (scheduleJson instanceof Map) {
			// [name: "scheduleName", holdType: ["nextTransition", "indefinite", "holdHours"], holdHours: number]
			if (scheduleJson.holdHours && scheduleJson.holdType && scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize(), scheduleJson.holdType?.trim(), scheduleJson.holdHours?.trim())
			} else if (scheduleJson.holdType && scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize(), scheduleJson.holdType?.trim())
			} else if (scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize())
			}
		} else if (scheduleJson instanceof List) {
			// [scheduleName, holdType, holdHours]
			setThermostatProgram( scheduleJson[0].trim().capitalize(), scheduleJson[1]?.trim(), scheduleJson[2]?.trim() )
		} else if (scheduleJson instanceof CharSequence) {
			// Simple scheduleName
			setThermostatProgram( scheduleJson.trim().capitalize() )
		} else {
			LOG("setSchedule( ${scheduleJson} ): Invalid argument Class - must be String, Map or List", 2, this, 'warn')
		}
	} else {
		def programsList = []
		def programs = device.currentValue('programsList')
		if (!programs) {
			LOG("Supported programs list not initialized, possible installation error", 1, this, 'warn')
			programsList = ['Away','Home','Sleep','Resume']		// Just use the default list
		} else {
			programsList = new JsonSlurper().parseText(programs) + ['Resume']
		}
		LOG("setSchedule( null ): Missing or Invalid argument - must be one of (${programsList.toString()[1..-2]})", 1, this, 'warn')
	}
}
void present(){
	// Change the Comfort Setting to Home (Nest compatibility)
	LOG('present()', 5, null, 'trace')
	setThermostatProgram('Home')
}
void homeDisabled() {}
void home() {
	// Change the Comfort Setting to Home
	LOG('home()', 5, null, 'trace')
	setThermostatProgram('Home')
}
void awayDisabled() {}
void away() {
	// Change the Comfort Setting to Away
	LOG('away()', 5, null, 'trace')
	setThermostatProgram('Away')
}
// Unfortunately, we can't overload the internal Java/Groovy/system definition of 'sleep()'
/*
void sleep() {
	// Change the Comfort Setting to Sleep
	LOG("sleep()", 5)
	setThermostatProgram("Sleep")
	return
}
*/
void asleep() {
	// Change the Comfort Setting to Sleep
	LOG('asleep()', 5, null, 'trace')
	setThermostatProgram('Sleep')
}
void nightDisabled() {}
void night() {
	// Change the Comfort Setting to Sleep
	LOG('night()', 5, null, 'trace')
	setThermostatProgram('Sleep')
}
void wakeup() { setThermostatProgram('Wakeup') }	// Not all thermostats have these two - setTP will validate
void awake() { setThermostatProgram('Awake') }
void resumeProgram(resumeAll=true) {
	resumeProgramInternal(resumeAll)
}
def resumeProgramInternal(resumeAll=true) {
	boolean result = true
	String thermostatHold = device.currentValue('thermostatHold', true)
	if ((thermostatHold == null) || (thermostatHold == '') || (thermostatHold == 'null')) {
		LOG('resumeProgram() - No current hold', 2, null, 'info')
		return result
	} else if (thermostatHold =='vacation') { // this shouldn't happen anymore - button changes to Cancel when in Vacation mode
		LOG('resumeProgram() - Cannot resume from Vacation hold', 2, null, 'error')
		return false
	} else {
		LOG("resumeProgram() - Hold type is ${thermostatHold}", 4)
	}

	sendEvent(name: 'thermostatStatus', value: 'Resuming scheduled Program...', displayed: false, isStateChange: true)

	def deviceId = getDeviceId()
	//sendEvent(name: 'thermostatStatus', value: 'Resuming Scheduled Program', displayed: false, isStateChange: true)
	if (parent.resumeProgram(this, deviceId, resumeAll)) {
    	String schedProgramName = device.currentValue('scheduledProgramName', true)
		def updates = [[ currentProgramName: schedProgramName, schedule: schedProgramName, holdStatus: 'null', thermostatHold: 'null', holdEndsAt: 'null', holdEndDate: 'null', schedText: 'null' ]]
		generateEvent(updates)
		sendEvent(name: 'thermostatStatus', value: 'Resume Program succeeded', displayed: false, isStateChange: true)
		LOG("resumeProgram(${resumeAll}) - succeeded", 2,null,'info')
		runIn(5, refresh, [overwrite:true])
	} else {
		sendEvent(name: "thermostatStatus", value: "Resume Program failed...", displayed: false, isStateChange: true)
		LOG("resumeProgram() - failed (parent.resumeProgram(this, ${deviceId}, ${resumeAll}))", 1, null, "error")
		result = false
	}
	// sendEvent(name: 'thermostatStatus', value: '', displayed: false, isStateChange: true)
	return result
}
void generateProgramEvent(String program, String failedProgram='') {
	LOG("Generate generateProgramEvent Event: program ${program}", 4)

	sendEvent(name: "thermostatStatus", value: "Setpoint updating...", displayed: false, isStateChange: true)

	String prog = program.capitalize()
	String progHold = (failedProgram == '') ? prog : "Hold: "+prog
    // Quickly update the key hold status variables, even before we hear back from the thermostat/cloud
	// We have to wait for wait until Ecobee sends us the correct currentProgramId
	def updates = [[currentProgramName: progHold], [currentProgram: prog], [schedule: progHold]]
    if (failedProgram == '') {
    	updates << [thermostatHold: 'null', holdEndsAt: 'null', holdStatus: 'null', holdEndDate: 'null', schedText: 'null', thermostatStatus: 'null']
    } else {
    	updates << [thermostatHold:'hold', thermostatStatus: 'null']	// Have to let ESM update the rest (dates, etc.) 
    }
    //log.debug "generateProgramEvent: ${updates}"
	generateEvent(updates)
}

// ***************************************************************************
// Thermostat Fan Mode Commands
// Fan Mode CANNOT be changed while in Vacation mode
// ***************************************************************************
void setThermostatFanMode(String value, holdType=null, holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	LOG("setThermostatFanMode(${value})", 4)
	// "auto" "on" "circulate" "off"

	// Cannot change fan settings while in Vacation mode
	String currentThermostatHold = device.currentValue('thermostatHold', true) 
	if (currentThermostatHold == 'vacation') {
		LOG("setThermostatFanMode(${value}, ${holdType}) fan mode change requested but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
    value = value.trim()		// Hubitat Dashboard's thermostat template adds extra space to some fanModes (" on", at least).
	String setValue = value
	// This is to work around a bug in some SmartApps that are using fanOn and fanAuto as inputs here, which is wrong
	if (value == "on" || value == "fanOn" ) 					{ value = 'on'; 		setValue = "on" }
	else if (value == "auto" || value == "fanAuto" ) 			{ value = 'auto'; 		setValue = "auto" }
	else if (value == "circulate" || value == "fanCirculate")	{ value = 'circulate'; 	setValue = "circulate" }	// Ecobees don't have a 'circulate' mode, per se, uses fanMinOnTime
	else if (value == "off" || value == "fanOff") 				{ value = 'off'; 		setValue = "auto" }
	else {
		LOG("setThermostatFanMode() - Unrecognized Fan Mode: ${value}. Setting to 'auto'", 1, null, "error")
		setValue = "auto"
	}

	String currentProgramName 
	//String currentThermostatHold
    def currentFanMinOnTime
   	currentProgramName = device.currentValue('currentProgramName', true)
   	//currentThermostatHold = device.currentValue('thermostatHold', true)
    currentFanMinOnTime = (device.currentValue('fanMinOnTime', true) ?: 0) as Integer
    
	if (((currentThermostatHold != '') && (currentThermostatHold != null) && (currentThermostatHold != 'null')) || currentProgramName.startsWith('Hold') || currentProgramName.startsWith('Auto ')) {
		resumeProgramInternal(true)
		// refresh()
	}

	String currentFanMode = device.currentValue('thermostatFanMode', true)	// value AFTER we reset the Hold (should really read the scheduledProgram's fan values)
	def updates = []
	def results
	def nullMinOnTime = null

	def sendHoldType = null
	def sendHoldHours = null
	if (holdType) {
		sendHoldType = holdType
		if (holdType == 'holdHours') sendHoldHours = holdHours
	} else {
		sendHoldType =	whatHoldType()
		if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
			sendHoldHours = sendHoldType
			sendHoldType = 'holdHours'
		}
	}

	switch (value) {
		case 'on':
			results = false
			// if (currentFanMode != 'on') results = parent.setFanMode(this, 'on', nullMinOnTime, getDeviceId(), sendHoldType, sendHoldHours)
            if (currentFanMode != 'on') results = parent.setFanMode(this, 'on', currentFanMinOnTime, getDeviceId(), sendHoldType, sendHoldHours)
			if (results) {
				// pre-load the values that will (eventually) be sent back from the thermostat
				updates = [[thermostatFanMode:'on'],
						   [thermostatOperatingState:'fan only'],
						   [equipmentOperatingState:'fan only'],
						   [thermostatFanModeDisplay:'on']]
				if ((currentFanMode != 'on') && (currentFanMode != 'off')) {	// i.e. if 'auto' or 'circulate', then turning on will cause a Hold: Fan event
					updates << [currentProgramName:'Hold: Fan On', thermostatHold: 'fan on']
				} else {
					updates << [currentProgramName: device.currentValue('scheduledProgramName', true)]	// In case currentProgram wasn't updated yet by the above resumeProgram()
				}
				generateEvent(updates)
			}
			break;

		case 'auto':
			// Note: we now set fanMinOntime to 0 when changing to Auto. "Circulate" will set it to non-zero number
			def autoHold = false
			if (currentFanMode != 'auto') {
				results = parent.setFanMode(this, 'auto', 0, getDeviceId(), sendHoldType, sendHoldHours)
				autoHold = true
			} else {
				results = parent.setFanMinOnTime(this, deviceId, 0)
			}
			if (results) {
				updates = [[fanMinOnTime: 0],
						   [thermostatFanMode: 'auto'], 
						   [thermostatFanModeDisplay: 'auto']]
				if (autoHold) updates << [currentProgramName: 'Hold: Fan Auto', thermostatHold: 'fan auto']
				log.warn "${updates}"
				generateEvent(updates)
			} else {
				updates = [[thermostatFanMode: currentFanMode]]
				log.warn "${updates}"
				generateEvent(updates)
			}
			break;

		case 'off':
			// To turn the fan OFF, HVACMode must be 'off', and Fan Mode needs to be 'auto' mode with fanMinOnTime = 0
			def thermostatMode = device.currentValue('thermostatMode', true) 

			if (thermostatMode != 'off') {
				LOG("Request to change Fan Mode to 'off' while Thermostat Mode isn't 'off' (${thermostatMode}), Fan Mode set to 'auto' instead",2,null,'warn')
				updates = [[thermostatFanMode: device.currentValue('thermostatFanMode', true)]]
				generateEvent(updates)
				return
			}
			if (currentFanMode != 'off') {
				// this also sets fanMinOnTime to zero
				if (parent.setFanMode(this, 'off', 0, getDeviceId(), sendHoldType, sendHoldHours)) {
					updates = [ [thermostatFanMode: 'off'], 
							    [fanMinOnTime: 0],
							    [thermostatFanModeDisplay:'off']
							  ]
					generateEvent(updates)
				}
			}
			break;

		case 'circulate':
			def autoHold = false
			// For Ecobee thermostats, 'circulate' will be active any time fanMinOnTime != 0 and Fan Mode == 'auto'
			// For this implementation, we will use 'circulate' whenever fanMinOnTime != 0, and 'off' when fanMinOnTime == 0 && thermostatMode == 'off'
			// def fanMinOnTime = device.currentValue('fanMinOnTime', true)
			def fanTime = currentFanMinOnTime
			if ((fanTime == null) || (fanTime.toInteger() == 0)) {
				// 20 minutes/hour is roughly the default for HoneyWell thermostats (35%), upon which ST has modeled their thermostat implementation
				fanTime = 20
			}
			if ((currentFanMode == 'auto') || (currentFanMode == 'circulate')) {
				if (fanTime != currentFanMinOnTime) {
					results = parent.setFanMinOnTime(this, deviceId, fanTime)
				} else results = true
			} else {
				results = parent.setFanMode(this, setValue, fanTime, getDeviceId(), sendHoldType, sendHoldHours)
				autoHold = true
			}
			if (results) {
				updates = [ [fanMinOnTime:fanTime],
						    [thermostatFanMode: 'circulate'],
						    [thermostatFanModeDisplay: 'circulate']
						  ]
				if (autoHold) updates << [currentProgramName: 'Hold: Circulate', thermostatHold: 'fan circulate']
				generateEvent(updates)
			} else {
				updates = [[thermostatFanMode: currentFanMode], [thermostatFanModeDisplay: currentFanMode]]
				generateEvent(updates)
			}
			break;
	}
}

String ventModes() {
	return new groovy.json.JsonBuilder(["auto", "minontime", "off", "on"]).toString()
}
String fanModes() {
	return new groovy.json.JsonBuilder(['auto','circulate','off','on']).toString() 
}
void fanOn() {
	LOG('fanOn()', 5, null, 'trace')
	setThermostatFanMode('on')
}
void fanAuto() {
	LOG('fanAuto()', 5, null, 'trace')
	setThermostatFanMode('auto')
}
void fanCirculate() {
	LOG('fanCirculate()', 5, null, 'trace')
	setThermostatFanMode('circulate')
}
void fanOffDisabled() {}
void fanOff() {
	LOG('fanOff()', 5, null, 'trace')
    fanMode = device.currentValue('thermostatFanMode', true)
	if (fanMode != 'off') {
		setThermostatFanMode('off')
		generateQuickEvent('setFanOff', 'off')		// reset the button icon state
	} else {
		LOG("Thermostat Fan Mode is 'off' (already)",2,null,'info')
		generateQuickEvent('setFanOff', 'off')		// reset the button icon state
	}
}

void setFanMinOnTimeDelay(minutes) {
	LOG("Slider requested Minutes: ${minutes}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sFMOT', [data: [mins:minutes]] )
}
void sFMOT(data) {
	LOG("Setting fan minutes to: ${data.mins}",4,null,'trace')
	setFanMinOnTime(data.mins)
}
void setFanMinOnTime(minutes=20) {
	
	def thermostatHold = device.currentValue('thermostatHold', true)
	if ( thermostatHold == 'vacation') {
		LOG("setFanMinOnTime() requested but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	minutes = minutes as Integer
	LOG("setFanMinOnTime(${minutes})", 4, null, "trace")
	def fanMinOnTime = device.currentValue('fanMinOnTime', true)
	LOG("Current fanMinOnTime: ${fanMinOnTime}, requested: ${minutes}",3,null,'info')
	if (fanMinOnTime?.toInteger() == minutes) return // allready there - all done!

	def deviceId = getDeviceId()
	if ((minutes >=0) && (minutes <=  55)) {
		if (parent.setFanMinOnTime(this, deviceId, minutes)) {
			def updates = [[fanMinOnTime: minutes]]
			def currentFanMode = device.currentValue('thermostatFanMode', true)
			if ((minutes == 0) && ((currentFanMode == 'circulate') || (currentFanMode == 'auto'))) {
				updates << [thermostatFanMode: 'auto', thermostatFanModeDisplay: 'auto']
			} else if ((minutes > 0) && (currentFanMode != 'circulate')) {
				updates << [thermostatFanMode: 'circulate', thermostatFanModeDisplay: 'circulate']
			}
			generateEvent(updates)
		}
	} else {
		LOG("setFanMinOnTime(${minutes}) - invalid argument",1,null, 'error')
	}
}
void setFanSpeed(String fanSpeed) {
	// List fanSpeedOptions = ['low','medium','high','optimized']
	List fanSpeedOptions = new JsonSlurper().parseText(device.currentValue('fanSpeedOptions',true))
	if (fanSpeedOptions.contains(fanSpeed) && (fanSpeed != "high")) {
		setEcobeeSetting('fanSpeed', fanSpeed)
	} else if (fanSpeed == 'high') {				// work around for Ecobee idiosyncracy
		if (fanSpeedOptions.contains('medium')) {	
			setEcobeeSetting('fanSpeed', 'high')	 
		} else {
			setEcobeeSetting('fanSpeed', 'medium')	// medium turns on high if only 2 speeds exist
		}
	} else {
		LOG("setFanSpeed(): '${fanSpeed}' is not a valid option for this thermostat ${fanSpeedOptions}",1,null,'warn')
	}
}
// Humidifier/Dehumidifier Commands
void setHumidifierMode(String value) {
	// verify thermostat hasHumidifier first
	def hasHumidifier = device.currentValue('hasHumidifier')
	if (!hasHumidifier || (hasHumidifier == 'false')) {
		LOG("${device.displayName} is not controlling a humidifier", 1, null, 'warn')
		return
	}
	if (!value	|| ((value != 'auto')  && (value != 'off') && (value != 'manual') && (value != 'on'))) {
		LOG("setHumidifierMode(${value}) - unsupported humidifier mode requested")
		return
	}
	if (value == 'on') value = 'manual'
	String humidifierMode = device.currentValue('humidifierMode', true)
	if (humidifierMode == value) {
		LOG("${device.displayName}'s humidifier is already set to ${value}", 2, null, 'info')
		return
	}
	def result = parent.setHumidifierMode(this, value, getDeviceId())
	if (result) {
		def updates = [[humidifierMode: value]]
		generateEvent(updates)
		LOG("${device.displayName}'s humidifier is now set to ${value}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		humidifierMode = device.currentValue('humidifierMode', true)
		LOG("Changing ${device.displayName}'s humidifier to ${value} failed, humidifier is still ${humidifierMode}", 1, null, 'warn')
	}
}

void setHumiditySetpoint(setpoint) {
	LOG("Humidity setpoint change to ${setpoint} requested", 2, null, 'trace')
	def dehumSP = device.currentValue('dehumiditySetpoint', true)
	if ((dehumSP != null) && dehumSP.toString().isNumber() && (dehumSP < setpoint)) LOG('Request to set Humidify Setpoint higher than Dehumidify Setpoint',1,null,'warn')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sHumSP', [data: [setpoint:setpoint]] )
}
void sHumSP(data) {
	LOG("Setting humidity setpoint to: ${data.setpoint}",4,null,'trace')
	setHumiditySetpointDelay(data.setpoint)
}
void setHumiditySetpointDelay(setpoint) {
	LOG("setHumiditySetpointDelay ${setpoint}",4,null,'trace')
	// verify that the stat hasDehumidifer
	def hasHumidifier = device.currentValue('hasHumidifier')
	if (!hasHumidifier || (hasDehumidifier == 'false')) {
		LOG("${device.displayName} is not controlling a humidifier", 1, null, 'warn')
		return
	}
	// log.debug "${device.currentValue('humiditySetpoint')}"
	def currentSetpoint = device.currentValue('humiditySetpoint', true)
	def humidifierMode  = device.currentValue('humidifierMode', true)
	if (humidifierMide == 'auto') {
		LOG("${device.displayName} is in Auto mode - cannot override Humidity setpoint", 1, null, 'warn')
		def updates = [[forced: true], [humiditySetpoint: currentSetpoint], [forced: false]]
		generateEvent(updates)
		return
	}
	if (currentSetpoint == setpoint) {
		LOG("${device.displayName}'s humidifier setpoint is already set to ${setpoint}", 2, null, 'info')
		return
	}
	def result = parent.setHumiditySetpoint(this, setpoint, getDeviceId())
	if (result) {
		LOG("${device.displayName}'s humidifier setpoint is now set to ${setpoint}", 2, null, 'info')
		def updates = [[humiditySetpoint: setpoint]]
		generateEvent(updates)
	} else {
		def updates = [[humiditySetpoint: currentSetpoint]]
		generateEvent(updates)
		LOG("Changing ${device.displayName}'s humidifier setpoint to ${setpoint} failed, setpoint is still ${currentSetpoint}", 1, null, 'warn')
	}
}

void setDehumidifierMode(String value) {
	// verify that the stat hasDehumidifer
	def hasDehumidifier = device.currentValue('hasDehumidifier')
	if (!hasDehumidifier || (hasDehumidifier == 'false')) {
		LOG("${deviceId.displayName} is not controlling a dehumidifier or overcooling", 1, child, 'warn')
		return
	}

	if (!value || (value != 'off') && (value != 'on')) {
		LOG("setHumidifierMode(${value}) - unsupported dehumidifier mode requested")
		return
	}
	def dehumidifierMode = device.currentValue('dehumidifierMode', true)
	if (dehumidifierMode == value) {
		LOG("${device.displayName}'s dehumidifier is already set to ${value}", 2, null, 'info')
	}
	def result = parent.setDehumidifierMode(this, value, getDeviceId())
	if (result) {
		def updates = [[dehumidifierMode: value]]
		generateEvent(updates)
		LOG("${device.displayName}'s dehumidifier is now set to ${value}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		LOG("Changing ${device.displayName}'s dehumidifier to ${value} failed, dehumidifier is still ${dehumidifierMode}", 1, null, 'warn')
	}
}
void setDehumiditySetpoint(setpoint) {
	LOG("Dehumidity setpoint change to ${setpoint} requested", 2, null, 'trace')
	def humiditySetpoint = device.currentValue('humiditySetpoint', true)
	if ((humiditySetpoint != null) && humiditySetpoint.toString().isNumber() && (humiditySetpoint > setpoint)) LOG('Request to set Dehumidify Setpoint lower than Humidify Setpoint',1,null,'warn')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sDehumSP', [data: [setpoint:setpoint]] )
}
void sDehumSP(data) {
	LOG("Setting dehumidity setpoint to: ${data.setpoint}",4,null,'trace')
	setDehumiditySetpointDelay(data.setpoint)
}
void setDehumiditySetpointDelay(setpoint) {
	LOG("setDehumiditySetpointDelay ${setpoint}",4,null,'trace')
	// verify that the stat hasDehumidifer
	def hasDehumidifier = device.currentValue('hasDehumidifier')
	if (!hasDehumidifier || (hasDehumidifier == 'false')) {
		LOG("${device.displayName} is not controlling a dehumidifier nor is it overcooling", 1, child, 'warn')
		return
	}
	def currentSetpoint = device.currentValue('dehumiditySetpoint', true)
	if (currentSetpoint == setpoint) {
		LOG("${device.displayName}'s dehumidifier setpoint is already set to ${setpoint}", 2, null, 'info')
		return
	}
	def result = parent.setDehumiditySetpoint(this, setpoint, getDeviceId())
	if (result) {
		def updates = [[dehumiditySetpoint: setpoint]]
		generateEvent(updates)
		LOG("${device.displayName}'s dehumidifier setpoint is now set to ${setpoint}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		LOG("Changing ${device.displayName}'s dehumidifier setpoint to ${setpoint} failed, setpoint is still ${currentSetpoint}", 1, null, 'warn')
	}
}

// Vacation commands
void setVacationFanMinOnTime(minutes=0) {
	
	def thermostatHold = device.currentValue('thermostatHold', true)
	if ( thermostatHold != 'vacation') {
		LOG("setVacationFanMinOnTime() requested but thermostat is not in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	minutes = minutes as Integer
	LOG("setVacationFanMinOnTime(${minutes})", 5, null, "trace")
	def fanMinOnTime = device.currentValue('fanMinOnTime', true)
	LOG("Current fanMinOnTime: ${fanMinOnTime}, requested: ${minutes}",3,null,'info')
	if (fanMinOnTime?.toInteger() == minutes) return // allready there - all done!
	def deviceId = getDeviceId()
	if ((minutes >=0) && (minutes <=  55)) {
		if (parent.setVacationFanMinOnTime(this, deviceId, minutes)) {
			def updates = [[fanMinOnTime: minutes]]
			generateEvent(updates)
		}
	} else {
		LOG("setVacationFanMinOnTime(${minutes}) - invalid argument",1,null, "error")
	}
}

// deleteVacation can delete any named vacation, or the currently running one (vacationName==null)
void deleteVacation(vacationName = null) {
	LOG("deleteVacation(${vacationName})", 5, null, "trace")
	if (vacationName == null) {
		cancelVacation()
		return
	}
	def deviceId = getDeviceId()
	parent.deleteVacation(this, deviceId, vacationName)
}

// cancelVacation will only cancel the currently running vacation
void cancelVacation() {
	LOG("cancelVacation()", 3, null, 'trace')
	
	def thermostatHold = device.currentValue('thermostatHold', true)
	if ( thermostatHold != 'vacation') {
		LOG("cancelVacation() - current hold is ${thermostatHold}",2,null,'trace')
		// Maybe we should do a resumeProgram first? But that could kill a Hold: if this is called while there is no vacation
		return
	}
	def deviceId = getDeviceId()
	if (parent.deleteVacation(this, deviceId, null)) {
        //log.debug "parent.deleteVacation succeeded"
        def updates = [[currentProgram:         (device.currentValue('scheduledProgram', true))],
                       [currentProgramName:     (device.currentValue('scheduledProgramName', true))],
                       [currentProgramId:       (device.currentValue('scheduledProgramId', true))],
                       [currentProgramOwner:    (device.currentValue('scheduledProgramOwner', true))],
                       [currentProgramType:     (device.currentValue('scheduledProgramType', true))]
                      ]
        generateEvent(updates)
    } else {
        LOG("cancelVacation failed", 1, null, 'warn')
    }
}
// Cancel a demandResponse Event
void cancelDemandResponse() {
	LOG('cancelDemandResponse()', 3, null, 'trace')
	def result = false
    def currentProgram = device.currentValue('currentProgram', true)
    if (currentProgram) {
    	if ((currentProgram == 'Eco!') || (currentProgram == 'Eco+!')) {
        	// Mandatory DR, can't cancel it
            LOG('Invalid request to Cancel Demand Response - current DR Event is MANDATORY', 1, null, 'warn')
            return
        }
    	if ((currentProgram == 'Eco') || (currentProgram == 'Eco+')) result = parent.cancelDemandResponse(this, getDeviceId())
    }
    if (result) {
        LOG('Cancel Demand Response Event succeeded.', 3, null, 'info')
    } else {
    	LOG("Cancel Demand Response Event FAILED${(currentProgram != 'Eco/Eco+') ? ' - not currently in a Demand Response Event' : ''}.", 1, null, 'warn')
    }
    return
}

// Climate change commands
void setProgramSetpoints(Object... programData) {
	def scale = temperatureScale
	LOG("setProgramSetpoints( ${programData} )",2,null,'info')
	String deviceId = getDeviceId()
	List<String> programDataList = programData as List<String>
	if (parent.setProgramSetpoints( this, deviceId, programDataList)) {
    	LOG("setProgramSetpoints() SUCCEEDED!!!",2,null,'trace')
        String currentProgram = device.currentValue('currentProgram', true)
		for (int i = 0; i < programDataList.size(); i += 3) {
            String programName = programDataList[i].toString()
			if ( currentProgram == programName) { 
                String heatSP = programDataList[i+1].toString()
                String coolSP = programDataList[i+2].toString()
				def updates = []
				if (coolSP) updates << [coolingSetpoint: coolSP]
				if (heatSP) updates << [heatingSetpoint: heatSP]
				if (updates != []) generateEvent(updates)
                break
			}
		}
		LOG("setProgramSetpoints() - completed",3,null,'trace')
	} else LOG("setProgramSetpoints() - failed",1,null,'warn')
}
void setEcobeeSetting( String name, value ) {
	def result
	def dItem = EcobeeDirectSettings.find{ it.name == name }
	if (dItem != null) {
		LOG("setEcobeeSetting( ${name}, ${value} ) - calling ${dItem.command}( ${value} )", 2, child, 'info')
		result = "${dItem.command}"(value)
	} else {
		String deviceId = getDeviceId()
		result = parent.setEcobeeSetting( this, deviceId, name, value as String)
        if (result) {
        	def updates = [["${name}": value as String]]
			generateEvent(updates)
        }
	}
	if (result == true){
		LOG("setEcobeeSetting( ${name}, ${value} ) completed.", 2, null, 'info')
	} else {
		LOG("setEcobeeSetting( ${name}, ${value} ) FAILED.", 1, null, 'warn')
	}
}

// microphone Off/On commands for Ecobee 4+
// If microphoneEnabled is null, then it is not supported on the current thermostat device, so we don't set a value here
void microphoneOff() {
	def isOn = device.currentValue('microphoneEnabled', true)
    if ((isOn != null) && ((isOn == true) || (isOn == 'true'))) {
    	setEcobeeSetting( 'microphoneEnabled', 'true')
    }
}
void microphoneOn() {
	def isOn = device.currentValue('microphoneEnabled', true)
    if ((isOn != null) && ((isOn == false) || (isOn == 'false'))) {
    	setEcobeeSetting( 'microphoneEnabled', 'false')
    }
}
// No longer used as of v1.2.21
void generateSetpointEvent() {
	LOG('generateSetpointEvent called in error',1,null,'error')
}

// This just updates the generic multiAttributeTile - text should match the Thermostat mAT
void generateStatusEvent() {
	LOG('generateStatusEvent called in error',1,null,'warn')
	// no longer needed, alternate multiAttributeTile no longer necessary
}

// generate custom mobile activity feeds event
// (Need to clean this up to remove as many characters as possible, else it isn't readable in the Mobile App
void generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "${device.displayName} ${notificationMessage}", descriptionText: "${device.displayName} ${notificationMessage}", displayed: true, isStateChange: true)
}

void noOp(arg=null) {
	// Doesn't do anything. Here due to a formatting issue on the Tiles!
}

def getSliderRange() {
	// should be returning the attributes heatRange and coolRange (once they are populated), but you can't get access to those while the forms are created (even after running for days).
	// return wantMetric() ? "(5..35)" : "(45..95)"
	return "(5..90)"
}

// Built in functions from SmartThings
// temperatureScale
// fahrenheitToCelsius()
// celsiusToFahrenheit()

boolean wantMetric() {
	return (temperatureScale == 'C')
}

def cToF(temp) {
	return celsiusToFahrenheit(temp)
}
def fToC(temp) {
	return fahrenheitToCelsius(temp)
}

def roundC(value) {
	// return (((value.toDouble() * 2.0).toDouble().round(0)) / 2.0).toDouble().round(1)
	return roundIt((roundIt((value * 2.0), 0) / 2.0), 1)
}

def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

String getDeviceId() {
	def deviceId = device.deviceNetworkId.split(/\./).last()
	LOG("getDeviceId() returning ${deviceId}", 4)
	return deviceId
}

boolean usingSmartAuto() {
	LOG("Entered usingSmartAuto() ", 5)
	if (settings.smartAuto) { return settings.smartAuto }
	if (getParentSetting('smartAuto')) return true	// { return parent.settings.smartAuto }
	return false
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. device preferences, 2. parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
def whatHoldType() {
	String theHoldType = settings.myHoldType
	def sendHoldType
	String parentHoldType
	switch (settings.myHoldType) {
		case 'Temporary':
			sendHoldType = 'nextTransition'
			break;
		case 'Permanent':
			sendHoldType = 'indefinite'
			break;
		case 'Hourly':
			parentHoldType = getParentSetting('holdType')
			if (settings.myHoldHours) {
				sendHoldType = myHoldHours as Integer
			} else if ((parentHoldType != null) && (parentHoldType == 'Specified Hours') && (getParentSetting('holdHours') != null)) {
				sendHoldType = getParentSettings('holdHours')
			} else if ( parentHoldType == '2 Hours') {
				sendHoldType = 2
			} else if ( parentHoldType == '4 Hours') {
				sendHoldType = 4
			} else {
				sendHoldType = 2
			}
			break;
		case "":
		case null :		// null means use parent value
			theHoldType = 'Parent'
		case 'Parent':
			parentHoldType = getParentSetting('holdType')
			if ((parentHoldType != null) && (parentHoldType == 'Thermostat Setting')) theHoldType = 'Thermostat'
		case 'Thermostat':
			if (theHoldType == 'Thermostat') {
				def holdType = device.currentValue('statHoldAction', true)
				switch(holdType) {
					case 'useEndTime4hour':
						sendHoldType = 4
						break;
					case 'useEndTime2hour':
						sendHoldType = 2
						break;
					case 'nextPeriod':
					case 'nextTransition':
						sendHoldType = 'nextTransition'
						break;
					case 'indefinite':
					case 'askMe':
					case null :
					case "":
					default :
						sendHoldType = 'indefinite'
						break;
				}
			} else {		// assert (theHoldType == Parent)
				switch (parentHoldType) {
					case 'Until I Change':
						sendHoldType = 'indefinite'
						break;
					case 'Until Next Program':
						sendHoldType = 'nextTransition'
						break;
					case '2 Hours':
						sendHoldType = 2
						break;
					case '4 Hours':
						sendHoldType = 4
						break;
					case 'Specified Hours':
						sendHoldType = getParentSetting('holdHours')?: 2
						break;
					case null :		// if not set in Parent, should probably use Thermostat setting, but precendence was set that null = indefinite
					case "":
					default :
						sendHoldType = 'indefinite'
						break;
				}
			}
	}
	if (sendHoldType) {
		LOG("Using holdType ${(sendHoldType?.toString()?.isNumber())?'holdHours ('+sendHoldType.toString()+')':sendHoldType}",2,null,'info')
		return sendHoldType
	} else {
		LOG("Couldn't determine holdType, returning indefinite",1,null,'error')
		return 'indefinite'
	}
}

void makeReservation(String childId, String type='modeOff') {
	LOG("The thermostat-based Reservation System has been deprecated - please recode for Ecobee Suite Manager-based implementation.",1,null,'error')
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
}

void cancelReservation( String childId, String type='modeOff') {
	LOG("The thermostat-based Reservation System has been deprecated - please recode for Ecobee Suite Manager-based implementation.",1,null,'error')
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
}

Integer howManyHours() {
	Integer sendHoldHours = 2
	if ((holdHours != null) && holdHours.toString().isNumber()) {
		sendHoldHours = holdHours.toInteger()
		LOG("Using ${device.displayName} holdHours: ${sendHoldHours}",2,this,'info')
	} else {
		def hh = getParentSetting('holdHours')
		if ((hh != null) && hh.toString().isNumber()) {
			sendHoldHours = hh as Integer
			LOG("Using ${parent.displayName} holdHours: ${sendHoldHours}",2,this,'info')
		}
	}
	return sendHoldHours
}

int getDebugLevel() {
	return (getDataValue('debugLevel') ?: (device.currentValue('debugLevel') ?: (getParentSetting('debugLevel') ?: 3))) as int
}
boolean debugLevel(level=3) {
	return ( getDebugLevel() >= (level as int) )
}

void LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = debugLevel(5) ? 'LOG: ' : ''
	if (logType == null) logType = 'debug'

	if (debugLevel(level)) {
		log."${logType}" "${prefix}${message}"
		if (event) { debugEvent(message+" (${logType})", displayEvent) }
	}
}

void debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent,
		isStateChange: true
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

def getTempColors() {
	def colorMap

	colorMap = [
/*		// Celsius Color Range
		[value: 0, color: "#1e9cbb"],
		[value: 15, color: "#1e9cbb"],
		[value: 19, color: "#1e9cbb"],

		[value: 21, color: "#44b621"],
		[value: 22, color: "#44b621"],
		[value: 24, color: "#44b621"],

		[value: 21, color: "#d04e00"],
		[value: 35, color: "#d04e00"],
		[value: 37, color: "#d04e00"],
		// Fahrenheit Color Range
		[value: 40, color: "#1e9cbb"],
		[value: 59, color: "#1e9cbb"],
		[value: 67, color: "#1e9cbb"],

		[value: 69, color: "#44b621"],
		[value: 72, color: "#44b621"],
		[value: 74, color: "#44b621"],

		[value: 76, color: "#d04e00"],
		[value: 95, color: "#d04e00"],
		[value: 99, color: "#d04e00"],
 */
							[value: 0, color: "#153591"],
							[value: 7, color: "#1e9cbb"],
							[value: 15, color: "#90d2a7"],
							[value: 23, color: "#44b621"],
							[value: 28, color: "#f1d801"],
							[value: 35, color: "#d04e00"],
							[value: 37, color: "#bc2323"],
							// Fahrenheit
							[value: 40, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"],

		[value: 451, color: "#ff4d4d"] // Nod to the book and temp that paper burns. Used to catch when the device is offline
	]
	return colorMap
}

def getStockTempColors() {
	def colorMap

	colorMap = [
		[value: 31, color: "#153591"],
		[value: 44, color: "#1e9cbb"],
		[value: 59, color: "#90d2a7"],
		[value: 74, color: "#44b621"],
		[value: 84, color: "#f1d801"],
		[value: 95, color: "#d04e00"],
		[value: 96, color: "#bc2323"]
	]
    return colorMap
}
// Ecobee Settings that must be changed only by specific commands
@Field final List EcobeeDirectSettings= [
											[name: 'fanMinOnTime',			command: 'setFanMinOnTime'],
											[name: 'dehumidifierMode',		command: 'setDehumidifierMode'],
                                            [name: 'dehumidiferLevel',		command: 'setDehumiditySetpoint'],
											[name: 'dehumidityLevel', 		command: 'setDehumiditySetpoint'],
											[name: 'dehumiditySetpoint',	command: 'setDehumiditySetpoint'],
											[name: 'humidifierMode',		command: 'setHumidifierMode'],
											[name: 'humiditySetpoint',		command: 'setHumiditySetpoint'],
											[name: 'schedule',				command: 'setSchedule']
										]
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return ((hubitat?.device?.HubAction == null) ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST() {
    return false    
}
boolean getIsHE() {
    return true
}

String getHubPlatform() {
    return "Hubitat"
}
boolean getIsSTHub() { return false }					// if (isSTHub) ...
boolean getIsHEHub() { return true }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return parent?."${settingName}"
}
@Field String  hubPlatform 	= getHubPlatform()
@Field boolean ST 			= false
@Field boolean HE 			= true
@Field String  debug		= 'debug'
@Field String  error		= 'error'
@Field String  info			= 'info'
@Field String  trace		= 'trace'
@Field String  warn			= 'warn'
