// ChickenSMP Season 2 promo (TikTok / YouTube Shorts)
import { backdrop, beats, echo, fact, hookFrame, payoff, punch } from './format.js';

export default {
  hook(s) {
    s.camera(punch(s, '#join', { zoom: 1.12 }));
    hookFrame(s, 'New *Minecraft* server?', { kicker: 'ChickenSMP' });
    echo(s, 'key-round', { color: 'yellow', at: -1 });
    s.image('assets/logo.png', { w: s.safe.w * 0.8, x: s.cx, y: s.safe.y + s.safe.h * 0.8, shadow: false, at: -1 });
    fact(s, '*Free* keys', { at: '#join', x: s.cx, y: s.safe.y + s.safe.h * 0.47 });
  },

  play(s) {
    s.camera(punch(s, '#phone', { zoom: 1.12 }));
    backdrop(s);
    beats(s, [
      { at: '#java', icon: 'monitor', label: 'Java Edition', color: 'green' },
      { at: '#bedrock', icon: 'gamepad-2', label: 'Bedrock', color: 'purple' },
      { at: '#phone', icon: 'smartphone', label: 'Even on mobile', color: 'blue' },
    ]);
  },

  rewards(s) {
    s.camera(punch(s, '#jackpot', { zoom: 1.12 }));
    backdrop(s, { glow: '#f59f00' });
    s.short.step(1, 2, { word: 'REWARDS' });
    beats(s, [
      { at: '#keyall', icon: 'key-round', label: 'Free key every hour', color: 'yellow' },
      { at: '#daily', icon: 'calendar-check', label: 'Daily rewards', color: 'green' },
      { at: '#jackpot', icon: 'trophy', label: 'Day 7 jackpot', color: 'orange' },
      { at: '#quests', icon: 'scroll-text', label: 'Quests', color: 'blue' },
    ]);
  },

  fun(s) {
    s.camera(punch(s, '#auction'));
    s.short.interrupt('#turn', { flash: false });
    backdrop(s, { glow: '#7cc4ff', pattern: 'rays' });
    s.short.step(2, 2, { word: 'REWARDS' });
    beats(s, [
      { at: '#turn', icon: 'sparkles', label: "There's more", color: 'yellow' },
      { at: '#bat', icon: 'bird', label: 'Money Bat', color: 'green' },
      { at: '#flip', icon: 'coins', label: 'Coinflip', color: 'yellow' },
      { at: '#auction', icon: 'gavel', label: 'Auction house', color: 'purple' },
    ]);
  },

  join(s) {
    s.camera(s.short.drift());
    const card = payoff(s, 'Join now', [
      { text: 'Season 2 just started', at: '#season' },
      { text: 'IP: chickensmp.net', at: '#ip' },
    ], { icon: 'gamepad-2' });
    const room = card.top - s.safe.y;
    s.image('assets/logo.png', { h: room * 0.62, x: s.cx, y: s.safe.y + room * 0.42, shadow: false, at: 0.1 });
  },
};
