import { registerPlugin } from '@capacitor/core';

import type { PedometerPluginPlugin } from './definitions';

const PedometerPlugin = registerPlugin<PedometerPluginPlugin>('PedometerPlugin', {
  web: () => import('./web').then((m) => new m.PedometerPluginWeb()),
});

export * from './definitions';
export { PedometerPlugin };
