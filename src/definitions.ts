export interface PedometerPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
