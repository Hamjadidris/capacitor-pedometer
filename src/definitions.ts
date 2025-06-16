export interface PedometerPlugin {
  /**
   * Checks if Android Health Connect or Apple HealthKit is available. If available will return `AVAILABLE`, otherwise it will return `UNAVAILABLE` or `NOTINSTALLED`.
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
   * Use Android's Sensor API to manually count, record and store steps in Health Connect. Requires `ACTIVITY_RECOGNITION` permission. Only available on Android.
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

  /**
   * Android only
   * Get changes token to watch for changes in Health Connect.
   * Even though you can get a token for multiple activity types. Android recommends getting separate tokens per activity type instead of getting them in bulk to avoid having an Exception in case one of the permissions is revoked.
   * @param activityType
   */
  getChangesToken(activityType: Activity[]): Promise<ChangesTokenResult>;

  /**
   * Android only
   * Get Health Connect changes
   * @param token
   */
  getChanges(token: string): Promise<ChangesResult>;
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
  dataOrigins?: string[];
}

export type QueryActivity = {
  id: string;
  startDate: string;
  endDate: string;
  value: number;
  dataOrigin?: string;
  sourceDevice?: Device;
};

export interface QueryActivityRequest {
  startDate: string;
  endDate: string;
  activityType: Activity;
  /**
   * If `between` is choose then startDate and endDate are required.
   * If `after` is choose then startDate is required.
   * If `before` is choose then endDate is required.
   * @default "between"
   *
   */
  filterType: Filter;
  /**
   * Only used on Android.
   */
  dataOriginFilter?: string[];
  limit?: number;
  ascending?: boolean;
  /**
   * Only used on Android.
   */
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

export interface ChangesTokenResult {
  token: string;
}

export interface Changes {
  type: 'upsert' | 'delete';
  record: QueryActivity[];
}

export interface ChangesResult {
  changes: Changes[];
  nextToken: string;
}
