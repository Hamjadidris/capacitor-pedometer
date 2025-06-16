# capacitor-pedometer

A plugin to make building your fitness apps just that easier, using Android Health Connect and Sensor Manager on Android and Apple HealthKit on IOS.

## Install

```bash
npm install capacitor-pedometer
npx cap sync
```

## Setup

### iOS

- Before you can use HealthKit, you must enable the HealthKit capabilities for your app. See https://developer.apple.com/documentation/healthkit/setting-up-healthkit
- Make sure you have the following keys in your Info.plist file:

```xml
<key>NSHealthShareUsageDescription</key>
<string> -Your reason for using HealthKit- </string>
<key>NSHealthUpdateUsageDescription</key>
<string> -Your reason for using HealthKit- </string>
```

### Android

- Add these in the application tag of your `AndroidManifest.xml`

```xml
<!-- For supported versions through Android 13, create an activity to show the rationale
       of Health Connect permissions once users click the privacy policy link. -->
        <activity
            android:name="com.hamjad.capacitor.pedometer.PermissionsRationaleActivity"
            android:exported="true">
          <intent-filter>
            <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
          </intent-filter>
        </activity>

        <!-- For versions starting Android 14, create an activity alias to show the rationale
            of Health Connect permissions once users click the privacy policy link. -->
        <activity-alias
            android:name="ViewPermissionUsageActivity"
            android:exported="true"
            android:targetActivity="com.hamjad.capacitor.pedometer.PermissionsRationaleActivity"
            android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
          <intent-filter>
            <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
            <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
          </intent-filter>
        </activity-alias>
```

- Declare the Health Connect package name in your `AndroidManifest.xml` after the application tag.

```xml
  <!-- Check if Health Connect is installed -->
    <queries>
        <package android:name="com.google.android.apps.healthdata" />
    </queries>
```

- Lastly, Add the permissions for the required data types such as;

```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_DISTANCE" />
<uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED" />
```

- Note that this plugin was built using gradle plugin verion `com.android.tools.build:gradle:8.7.2` which requires gradle version `8.9`

### Android Step Sensor

Because Health Connect acts more as central store for health and fitness data rather than a pedometer that tracks steps, This function was created for that exact purpose in mind. It combines Android's internal step `Sensor` with Google's `Recording API` which allows retrieving step count data from a mobile device in a battery-efficient way, which the function then stores in Health Connect.

#### Set up `Step Sensor`

- Add this in the application tag of your `AndroidManifest.xml`

```xml
   <activity
      android:name="com.hamjad.capacitor.pedometer.StepSensor"
      android:exported="true"
      android:theme="@android:style/Theme.Translucent.NoTitleBar">
      <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
      </intent-filter>
    </activity>
```

- Add permissions to use Android `Sensor` and write Steps data to Health Connect;

```xml
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.health.WRITE_STEPS" />
```

## API

<docgen-index>

* [`checkAvailability()`](#checkavailability)
* [`requestPermission()`](#requestpermission)
* [`checkPermission()`](#checkpermission)
* [`useStepSensor()`](#usestepsensor)
* [`queryActivity(...)`](#queryactivity)
* [`queryAggregatedActivity(...)`](#queryaggregatedactivity)
* [`getChangesToken(...)`](#getchangestoken)
* [`getChanges(...)`](#getchanges)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### checkAvailability()

```typescript
checkAvailability() => Promise<AvailabilityResult>
```

Checks if Android Health Connect or Apple HealthKit is available. If available will return `AVAILABLE`, otherwise it will return `UNAVAILABLE` or `NOTINSTALLED`.

**Returns:** <code>Promise&lt;<a href="#availabilityresult">AvailabilityResult</a>&gt;</code>

--------------------


### requestPermission()

```typescript
requestPermission() => Promise<PermissionResponse>
```

Requests permissions for steps, distance and calories

**Returns:** <code>Promise&lt;<a href="#permissionresponse">PermissionResponse</a>&gt;</code>

--------------------


### checkPermission()

```typescript
checkPermission() => Promise<PermissionResponse>
```

Check permissions that have been previously requested.

**Returns:** <code>Promise&lt;<a href="#permissionresponse">PermissionResponse</a>&gt;</code>

--------------------


### useStepSensor()

```typescript
useStepSensor() => Promise<SensorResponse>
```

Use Android's Sensor API to manually count, record and store steps in Health Connect. Requires `ACTIVITY_RECOGNITION` permission. Only available on Android.

**Returns:** <code>Promise&lt;<a href="#sensorresponse">SensorResponse</a>&gt;</code>

--------------------


### queryActivity(...)

```typescript
queryActivity(requestOptions: QueryActivityRequest) => Promise<QueryActivityResponse>
```

Query activity data

| Param                | Type                                                                  |
| -------------------- | --------------------------------------------------------------------- |
| **`requestOptions`** | <code><a href="#queryactivityrequest">QueryActivityRequest</a></code> |

**Returns:** <code>Promise&lt;<a href="#queryactivityresponse">QueryActivityResponse</a>&gt;</code>

--------------------


### queryAggregatedActivity(...)

```typescript
queryAggregatedActivity(requestOptions: QueryAggregatedActivityRequest) => Promise<QueryAggregatedActivityResponse>
```

Query aggregated activity data

| Param                | Type                                                                                      |
| -------------------- | ----------------------------------------------------------------------------------------- |
| **`requestOptions`** | <code><a href="#queryaggregatedactivityrequest">QueryAggregatedActivityRequest</a></code> |

**Returns:** <code>Promise&lt;<a href="#queryaggregatedactivityresponse">QueryAggregatedActivityResponse</a>&gt;</code>

--------------------


### getChangesToken(...)

```typescript
getChangesToken(activityType: Activity[]) => Promise<ChangesTokenResult>
```

Android only
Get changes token to watch for changes in Health Connect.
Even though you can get a token for multiple activity types. Android recommends getting separate tokens per activity type instead of getting them in bulk to avoid having an Exception in case one of the permissions is revoked.

| Param              | Type                    |
| ------------------ | ----------------------- |
| **`activityType`** | <code>Activity[]</code> |

**Returns:** <code>Promise&lt;<a href="#changestokenresult">ChangesTokenResult</a>&gt;</code>

--------------------


### getChanges(...)

```typescript
getChanges(token: string) => Promise<ChangesResult>
```

Android only
Get Health Connect changes

| Param       | Type                |
| ----------- | ------------------- |
| **`token`** | <code>string</code> |

**Returns:** <code>Promise&lt;<a href="#changesresult">ChangesResult</a>&gt;</code>

--------------------


### Interfaces


#### PermissionResponse

| Prop              | Type                                                                        |
| ----------------- | --------------------------------------------------------------------------- |
| **`allGranted`**  | <code>boolean</code>                                                        |
| **`permissions`** | <code><a href="#mappedpermissionrecords">mappedPermissionRecords</a></code> |


#### SensorResponse

| Prop         | Type             |
| ------------ | ---------------- |
| **`result`** | <code>any</code> |


#### QueryActivityResponse

| Prop             | Type                         |
| ---------------- | ---------------------------- |
| **`activities`** | <code>QueryActivity[]</code> |
| **`pageToken`**  | <code>string</code>          |


#### QueryActivityRequest

| Prop                   | Type                                          | Description                                                                                                                                                      | Default                |
| ---------------------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| **`startDate`**        | <code>string</code>                           |                                                                                                                                                                  |                        |
| **`endDate`**          | <code>string</code>                           |                                                                                                                                                                  |                        |
| **`activityType`**     | <code><a href="#activity">Activity</a></code> |                                                                                                                                                                  |                        |
| **`filterType`**       | <code><a href="#filter">Filter</a></code>     | If `between` is choose then startDate and endDate are required. If `after` is choose then startDate is required. If `before` is choose then endDate is required. | <code>"between"</code> |
| **`dataOriginFilter`** | <code>string[]</code>                         | Only used on Android.                                                                                                                                            |                        |
| **`limit`**            | <code>number</code>                           |                                                                                                                                                                  |                        |
| **`ascending`**        | <code>boolean</code>                          |                                                                                                                                                                  |                        |
| **`pageToken`**        | <code>string</code>                           | Only used on Android.                                                                                                                                            |                        |


#### QueryAggregatedActivityResponse

| Prop                 | Type                            |
| -------------------- | ------------------------------- |
| **`aggregatedData`** | <code>AggregatedSample[]</code> |


#### AggregatedSample

| Prop              | Type                  |
| ----------------- | --------------------- |
| **`startDate`**   | <code>string</code>   |
| **`endDate`**     | <code>string</code>   |
| **`value`**       | <code>number</code>   |
| **`dataOrigins`** | <code>string[]</code> |


#### QueryAggregatedActivityRequest

| Prop               | Type                                          |
| ------------------ | --------------------------------------------- |
| **`startDate`**    | <code>string</code>                           |
| **`endDate`**      | <code>string</code>                           |
| **`activityType`** | <code><a href="#activity">Activity</a></code> |
| **`bucket`**       | <code><a href="#bucket">Bucket</a></code>     |


#### ChangesTokenResult

| Prop        | Type                |
| ----------- | ------------------- |
| **`token`** | <code>string</code> |


#### ChangesResult

| Prop            | Type                   |
| --------------- | ---------------------- |
| **`changes`**   | <code>Changes[]</code> |
| **`nextToken`** | <code>string</code>    |


#### Changes

| Prop         | Type                              |
| ------------ | --------------------------------- |
| **`type`**   | <code>'upsert' \| 'delete'</code> |
| **`record`** | <code>QueryActivity[]</code>      |


### Type Aliases


#### AvailabilityResult

<code>{ result: 'AVAILABLE' | 'UNAVAILABLE' | 'NOTINSTALLED'; }</code>


#### mappedPermissionRecords

<code>{ [PermissionRecordKey in 'readSteps' | 'writeSteps' | 'distance' | 'calories']: boolean; }</code>


#### QueryActivity

<code>{ id: string; startDate: string; endDate: string; value: number; dataOrigin?: string; sourceDevice?: <a href="#device">Device</a>; }</code>


#### Device

<code>'UNKNOWN' | 'WATCH' | 'PHONE' | 'SCALE' | 'RING' | 'HEAD_MOUNTED' | 'FITNESS_BAND' | 'CHEST_STRAP' | 'SMART_DISPLAY'</code>


#### Activity

<code>'steps' | 'distance' | 'calories'</code>


#### Filter

<code>'between' | 'after' | 'before'</code>


#### Bucket

<code>'hour' | 'day' | 'weeks' | 'month'</code>

</docgen-api>
