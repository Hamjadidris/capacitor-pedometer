export interface PedometerPlugin {
  /**
   * Checks if Health API is available. If available will return `AVAILABLE` otherwise it will return UNAVAILABLE or NOTINSTALLED.
   *
   */
  checkAvailability(): Promise<AvailabilityResult>;

  /**
   *
   * Requests permissions for steps, distance and calories
   *
   */
  requestPermission(): Promise<PermissionResponse>;

  /**
   *
   * Check permissions that have been previously requested.
   *
   */
  checkPermission(): Promise<PermissionResponse>;

  /**
   *
   * Use Android's Sensor API to manually count, record and store steps in Health connect. Requires `ACTIVITY_RECOGNITION` permission. Only available on Android.
   *
   */
  useStepSensor(): Promise<SensorResponse>;

  /**
   * Query activity data
   * @param requestOptions
   */
  queryActivity(requestOptions: QueryActivityRequest): Promise<QueryActivityResponse>;

  /**
   * Query aggregated activity data
   * @param requestOptions
   */
  queryAggregatedActivity(requestOptions: QueryAggregatedActivityRequest): Promise<QueryAggregatedActivityResponse>;
}

export declare type AvailabilityResult = {
  result: 'AVAILABLE' | 'UNAVAILABLE' | 'NOTINSTALLED';
};

export declare type Activity = 'steps' | 'distance' | 'calories';
export declare type Bucket = 'hour' | 'day' | 'weeks' | 'month';
export declare type Filter = 'between' | 'after' | 'before';
export declare type Device =
  | 'UNKNOWN'
  | 'WATCH'
  | 'PHONE'
  | 'SCALE'
  | 'RING'
  | 'HEAD_MOUNTED'
  | 'FITNESS_BAND'
  | 'CHEST_STRAP'
  | 'SMART_DISPLAY';
export declare type mappedPermissionRecords = {
  [PermissionRecordKey in 'readSteps' | 'writeSteps' | 'distance' | 'calories']: boolean;
};

export interface PermissionResponse {
  allGranted: boolean;
  permissions: mappedPermissionRecords;
}
export interface SensorResponse {
  result: any;
}

export interface AggregatedSample {
  startDate: string;
  endDate: string;
  value: number;
  sourceName?: string;
}

export type QueryActivity = {
  id: string;
  startDate: string;
  endDate: string;
  value: number;
  sourceName: string;
  sourceDevice?: Device;
};

export interface QueryActivityRequest {
  startDate: string;
  endDate: string;
  activityType: Activity;
  filterType: Filter;
  dataOriginFilter?: string[];
  limit?: number;
  ascending?: boolean;
  pageToken?: string;
}

export interface QueryActivityResponse {
  activities: QueryActivity[];
  pageToken?: string;
}

export interface QueryAggregatedActivityRequest {
  startDate: string;
  endDate: string;
  activityType: Activity;
  bucket: Bucket;
}

export interface QueryAggregatedActivityResponse {
  aggregatedData: AggregatedSample[];
}
