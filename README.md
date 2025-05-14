# capacitor-pedometer

A plugin to make building your fitness apps just that easier, using Google Health Connect on Android and Apple HealthKit on IOS

## Install

```bash
npm install capacitor-pedometer
npx cap sync
```

## API

<docgen-index>

* [`checkAvailability()`](#checkavailability)
* [`requestPermission()`](#requestpermission)
* [`checkPermission()`](#checkpermission)
* [`useStepSensor()`](#usestepsensor)
* [`queryActivity(...)`](#queryactivity)
* [`queryAggregatedActivity(...)`](#queryaggregatedactivity)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### checkAvailability()

```typescript
checkAvailability() => Promise<AvailabilityResult>
```

Checks if Health API is available. If available will return `AVAILABLE` otherwise it will return `UNAVAILABLE` or `NOTINSTALLED` if user is on android and hasn't intalled Health Connect.

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

Use Android's Sensor API to manually count, record and store steps in Health connect. Requires `ACTIVITY_RECOGNITION` permission. Only available on Android.

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

| Prop             | Type                |
| ---------------- | ------------------- |
| **`startDate`**  | <code>string</code> |
| **`endDate`**    | <code>string</code> |
| **`value`**      | <code>number</code> |
| **`sourceName`** | <code>string</code> |


#### QueryAggregatedActivityRequest

| Prop               | Type                                          |
| ------------------ | --------------------------------------------- |
| **`startDate`**    | <code>string</code>                           |
| **`endDate`**      | <code>string</code>                           |
| **`activityType`** | <code><a href="#activity">Activity</a></code> |
| **`bucket`**       | <code><a href="#bucket">Bucket</a></code>     |


### Type Aliases


#### AvailabilityResult

<code>{ result: 'AVAILABLE' | 'UNAVAILABLE' | 'NOTINSTALLED'; }</code>


#### mappedPermissionRecords

<code>{ [PermissionRecordKey in 'readSteps' | 'writeSteps' | 'distance' | 'calories']: boolean; }</code>


#### QueryActivity

<code>{ id: string; startDate: string; endDate: string; value: number; sourceName?: string; sourceDevice?: <a href="#device">Device</a>; }</code>


#### Device

<code>'UNKNOWN' | 'WATCH' | 'PHONE' | 'SCALE' | 'RING' | 'HEAD_MOUNTED' | 'FITNESS_BAND' | 'CHEST_STRAP' | 'SMART_DISPLAY'</code>


#### Activity

<code>'steps' | 'distance' | 'calories'</code>


#### Filter

<code>'between' | 'after' | 'before'</code>


#### Bucket

<code>'hour' | 'day' | 'weeks' | 'month'</code>

</docgen-api>
