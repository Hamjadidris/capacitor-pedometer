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
                "steps" : HKQuantityType(.stepCount),
                "calories": HKQuantityType(.activeEnergyBurned),
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
    
    func getDateFromISOString(isoString: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime,
                                       .withDashSeparatorInDate,
                                       .withFullDate,
                                       .withFractionalSeconds,
                                       .withColonSeparatorInTimeZone]
        
        formatter.timeZone = TimeZone.current
        let date =  formatter.date(from: isoString)
        return date
    }
    
    func getISOStringFromDate(date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")

        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        formatter.timeZone = TimeZone(secondsFromGMT: TimeZone.current.secondsFromGMT())
        let isoString =  formatter.string(from: date)
        
        return isoString
    }
    
    func getFilterPredicate(startDate:Date, endDate:Date, filter: String) -> NSPredicate{
        switch filter {
        case "before":
            HKQuery.predicateForSamples(withStart: nil, end: endDate, options: .strictEndDate)
        case "after":
            HKQuery.predicateForSamples(withStart: startDate, end: nil, options: .strictStartDate)
        case "between":
            HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictEndDate)
        default:
            HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictEndDate)
        }
    }
    
    func getSampleValue(activityType: String, dataType: HKQuantityType, quantity: HKQuantity) -> Double{
        var value: Double = 0

        if(activityType == "steps" && dataType.is(compatibleWith: HKUnit.count())) {
            value = quantity.doubleValue(for: .count())
        } else if(activityType == "calories" && dataType.is(compatibleWith: HKUnit.kilocalorie())) {
            value = quantity.doubleValue(for: .kilocalorie())
        } else if(activityType == "distance" && dataType.is(compatibleWith: HKUnit.second())) {
            value = quantity.doubleValue(for: .meter())
        }
        
        return value
    }

    @objc func queryActivity(_ call: CAPPluginCall){
              let startDateString = call.getString("startDate")
              let endDateString = call.getString("endDate")
              let filterType = call.getString("filterType", "between")
              let limit = call.getInt("limit",  HKObjectQueryNoLimit)
              let ascending = call.getBool("ascending", true)
        
        guard let activityType = call.getString("activityType") else {
            call.reject("Invalid activity type")
            return
        }
        
        let permissionTypes = getPermissionTypes()
        
        guard let dataType = permissionTypes[activityType] else {
            call.reject("Invalid activity type")
            return
        }
        
        guard let startDate = getDateFromISOString(isoString: startDateString!) else{
            call.reject("Invalid start date provided")
            return
        }
        
        guard let endDate = getDateFromISOString(isoString: endDateString!) else{
            call.reject("Invalid end date provided")
            return
        }
        
        let predicate = getFilterPredicate(startDate: startDate, endDate: endDate, filter: filterType)
        
        let sortDescriptor = [NSSortDescriptor(key: "quantity", ascending: ascending)]

        let query = HKSampleQuery(sampleType: dataType,
                                     predicate: predicate,
                                     limit: limit ,
                                     sortDescriptors: sortDescriptor ) { _, samples, error in
            
            if let error = error {
                call.reject("\(error.localizedDescription)")
                return
            }
            
            guard let querySamples = samples as? [HKQuantitySample] else { return }
               
               var queryResult: [[String: Any]] = []
               
               for sample in querySamples {
                   let startDate = self.getISOStringFromDate(date: sample.startDate)
                   let endDate = self.getISOStringFromDate(date: sample.endDate)
                   
                   
                   
                   let value = self.getSampleValue(activityType: activityType, dataType: dataType, quantity: sample.quantity)
                   
                   let source = sample.sourceRevision
                   
                   
                
                queryResult.append([
                    "id": sample.uuid,
                    "startDate": startDate,
                    "endDate": endDate,
                    "value": value,
                    "dataOrigin": source.source.bundleIdentifier,
                    "sourceDevice": source.productType ?? "UNKNOWN"
                ])
               }
               
               call.resolve(["activities": queryResult])
           }

           HKHealthStore().execute(query)
    }
    
    let bucketTypes = [
        "hour": DateComponents(hour: 1),
        "day": DateComponents(day: 1),
        "weeks": DateComponents(weekOfYear: 1),
        "month": DateComponents(month: 1),
    ]

    @objc func queryAggregatedActivity(_ call: CAPPluginCall){
        guard let startDateString = call.getString("startDate"),
              let endDateString = call.getString("endDate"),
              let activityType = call.getString("activityType"),
              let bucket = call.getString("bucket") else {
                           call.reject("Invalid parameters")
                           return
                        }
        
        let permissionTypes = getPermissionTypes()
        
        guard let dataType = permissionTypes[activityType] else {
            call.reject("Invalid activity type")
            return
        }
        
        guard let interval = bucketTypes[bucket] else {
                call.reject("Invalid bucket type")
                return
        }
        
        guard let startDate = getDateFromISOString(isoString: startDateString) else{
            call.reject("Invalid start date provided")
            return
        }
        
        guard let endDate = getDateFromISOString(isoString: endDateString) else{
            call.reject("Invalid end date provided")
            return
        }
        
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictEndDate)
        
        let query = HKStatisticsCollectionQuery(quantityType: dataType,
                                                   quantitySamplePredicate: predicate,
                                                   options: [.cumulativeSum, .separateBySource],
                                                   anchorDate: startDate,
                                                   intervalComponents: interval)

        query.initialResultsHandler = { query, result, error in
            if let error = error {
                call.reject("Error fetching aggregated data: \(error.localizedDescription)")
                return
            }
            
            var aggregatedSamples: [[String: Any]] = []
            
            result?.enumerateStatistics(from: startDate, to: endDate) { statistics, stop in
                
                if let sum = statistics.sumQuantity() {
                    let startDate = self.getISOStringFromDate(date: statistics.startDate)
                    let endDate = self.getISOStringFromDate(date: statistics.endDate)
                    
                    let value = self.getSampleValue(activityType: activityType, dataType: dataType, quantity: sum)
                    
                    var sourcesArray:Array = []
                        
                    statistics.sources?.forEach(){ source in
                        sourcesArray.append(source.bundleIdentifier)
                    }
                    
                    aggregatedSamples.append([
                        "startDate": startDate,
                        "endDate": endDate,
                        "value": value,
                        "dataOrigins": sourcesArray
                    ])
                }
            }
            
            call.resolve(["aggregatedData": aggregatedSamples])
        }

        HKHealthStore().execute(query)
    }
}
