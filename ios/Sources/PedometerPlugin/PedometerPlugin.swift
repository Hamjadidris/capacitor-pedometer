import Foundation
import Capacitor
import HealthKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(PedometerPlugin)
public class PedometerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "PedometerPlugin"
    public let jsName = "Pedometer"

     public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkAvailability", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "useStepSensor", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "queryActivity", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "queryAggregatedActivity", returnType: CAPPluginReturnPromise)
    ]
    
    let healthStore = HKHealthStore()
    
    
    @objc func checkAvailability(_ call: CAPPluginCall) {
        let isAvailable = HKHealthStore.isHealthDataAvailable()
        let availabilityResult = isAvailable == true ? "AVAILABLE" : "UNAVAILABLE"
        call.resolve(["result": availabilityResult])
    }
    
    func getPermissionTypes() -> [String : HKQuantityType] {
        let permissionTypes: [String : HKQuantityType]
        
        if #available(iOS 15.0, *) {
            permissionTypes = [
                "steps" : HKQuantityType(.activeEnergyBurned),
                "calories": HKQuantityType(.stepCount),
                "distance": HKQuantityType(.distanceWalkingRunning),
            ]
        } else {
            // Fallback on earlier versions
            permissionTypes = [
                "steps": HKQuantityType.quantityType(forIdentifier: .stepCount)!,
                "calories": HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!,
                "distance": HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)!,
            ]
        }
        
        return permissionTypes
    }
    
    func getAuthorizationStatus(permissionType: HKObjectType) -> Bool {
        let status = healthStore.authorizationStatus(for: permissionType)

        return status == .sharingAuthorized
    }
    
    @objc func checkPermission(_ call: CAPPluginCall){
        let permissionTypes = getPermissionTypes()
        var authResultDict: [String: Bool] = [:]
        
        for (permissionName, permissionType) in permissionTypes {
            authResultDict[permissionName] = self.getAuthorizationStatus(permissionType: permissionType)
        }
        
        call.resolve([
            "permissions": [
                "readSteps" : authResultDict["steps"],
                 "writeSteps" : authResultDict["steps"],
                 "distance" : authResultDict["distance"],
                 "calories" : authResultDict["calories"]
            ],
            "allGranted": authResultDict["steps"]! && authResultDict["distance"]! && authResultDict["calories"]!
        ])
    }

    @objc func requestPermission(_ call: CAPPluginCall){
       
        let permissionTypes = getPermissionTypes()
        
        let permissionSet = Set(permissionTypes.values)
        print(permissionSet)
        
        healthStore.requestAuthorization(toShare: permissionSet, read: permissionSet){ (success, error) in
            if success {
                
                var authResultDict: [String: Bool] = [:]
                
                // Authorization successful
                for (permissionName, permissionType) in permissionTypes {
                    authResultDict[permissionName] = self.getAuthorizationStatus(permissionType: permissionType)
                }
                
                call.resolve([
                    "permissions": [
                        "readSteps" : authResultDict["steps"],
                         "writeSteps" : authResultDict["steps"],
                         "distance" : authResultDict["distance"],
                         "calories" : authResultDict["calories"]
                    ],
                    "allGranted": authResultDict["steps"]! && authResultDict["distance"]! && authResultDict["calories"]!
                ])
            } else {
                // Authorization failed
                call.reject("\(error?.localizedDescription ?? "Requesting permissions failed")")
            }
        }
    }

    @objc func useStepSensor(_ call: CAPPluginCall){
        call.reject("Step sensor is only implemented on Android")
    }

    @objc func queryActivity(_ call: CAPPluginCall){
        call.reject("Not implemented")
    }

    @objc func queryAggregatedActivity(_ call: CAPPluginCall){
        call.reject("Not implemented")
    }
}
