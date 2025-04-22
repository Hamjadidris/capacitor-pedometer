import { WebPlugin } from '@capacitor/core';

import type { PedometerPluginPlugin } from './definitions';

export class PedometerPluginWeb extends WebPlugin implements PedometerPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
