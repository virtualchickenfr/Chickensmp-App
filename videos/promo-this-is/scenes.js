// "This is ChickenSMP" promo (TikTok / YouTube Shorts)
import { backdrop, beats, echo, fact, hookFrame, payoff, punch } from './format.js';

export default {
  hook(s) {
    s.camera(punch(s, '#what', { zoom: 1.1 }));
    hookFrame(s, 'This is *ChickenSMP*', { kicker: 'Minecraft SMP' });
    s.image('assets/logo.png', { w: s.safe.w * 0.82, x: s.cx, y: s.safe.y + s.safe.h * 0.78, shadow: false, at: -1 });
    fact(s, 'Survival + *economy*', { at: '#what', x: s.cx, y: s.safe.y + s.safe.h * 0.47 });
  },

  economy(s) {
    s.camera(punch(s, '#fish', { zoom: 1.12 }));
    backdrop(s, { glow: '#f59f00' });
    beats(s, [
      { at: '#auction', icon: 'gavel', label: 'Auction house', color: 'purple' },
      { at: '#orders', icon: 'clipboard-list', label: 'Orders for cash', color: 'blue' },
      { at: '#fish', icon: 'fish', label: 'Fish worth millions', color: 'yellow' },
    ]);
  },

  pvp(s) {
    s.camera(punch(s, '#build', { zoom: 1.1 }));
    backdrop(s, { glow: '#e03131', pattern: 'rays' });
    beats(s, [
      { at: '#pvp', icon: 'swords', label: 'PvP area', color: 'red' },
      { at: '#build', icon: 'timer', label: 'Blocks vanish in 20 min', color: 'orange' },
    ]);
  },

  rewards(s) {
    s.camera(punch(s, '#key', { zoom: 1.12 }));
    backdrop(s, { glow: '#f59f00' });
    echo(s, 'key-round', { color: 'yellow', at: '#key' });
    beats(s, [
      { at: '#key', icon: 'key-round', label: 'Free key every hour', color: 'yellow' },
      { at: '#daily', icon: 'calendar-check', label: 'Daily rewards + quests', color: 'green' },
    ]);
  },

  fair(s) {
    s.camera(s.short.drift());
    backdrop(s, { glow: '#3b7bff' });
    beats(s, [
      { at: '#fair', icon: 'shield-check', label: 'Fair fights', color: 'blue' },
    ]);
  },

  community(s) {
    s.camera(punch(s, '#discord', { zoom: 1.1 }));
    backdrop(s, { glow: '#7c5cff' });
    beats(s, [
      { at: '#app', icon: 'smartphone', label: 'ChickenSMP app', color: 'blue' },
      { at: '#discord', icon: 'message-circle', label: '500+ on Discord', color: 'purple' },
    ]);
  },

  join(s) {
    s.camera(s.short.drift());
    const card = payoff(s, 'Join now', [
      { text: 'IP: chickensmp.net', at: '#ip' },
      { text: 'See you in game', at: '#see' },
    ], { icon: 'gamepad-2' });
    const room = card.top - s.safe.y;
    s.image('assets/logo.png', { h: room * 0.62, x: s.cx, y: s.safe.y + room * 0.42, shadow: false, at: 0.1 });
  },
};
