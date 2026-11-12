// ChickenSMP: we're hiring a Social Media Manager (TikTok / YouTube Shorts)
import { backdrop, beats, fact, hookFrame, payoff, punch } from './format.js';

export default {
  hook(s) {
    s.camera(punch(s, '#hiring', { zoom: 1.12 }));
    hookFrame(s, "We're *hiring*", { icon: 'megaphone', color: 'blue', kicker: 'ChickenSMP' });
    fact(s, '*Social Media* Manager', { at: '#hiring' });
  },

  what(s) {
    s.camera(punch(s, '#events', { zoom: 1.12 }));
    backdrop(s);
    s.short.step(1, 2, { word: 'JOB' });
    beats(s, [
      { at: '#tiktok', icon: 'smartphone', label: 'TikToks & Shorts', color: 'pink' },
      { at: '#clips', icon: 'clapperboard', label: 'Server clips', color: 'purple' },
      { at: '#grow', icon: 'trending-up', label: 'Grow our channels', color: 'green' },
      { at: '#events', icon: 'party-popper', label: 'Events & giveaways', color: 'yellow' },
    ]);
  },

  turn(s) {
    s.camera(punch(s, '#creators'));
    s.short.interrupt('#turn', { flash: false });
    backdrop(s, { glow: '#7cc4ff', pattern: 'rays' });
    s.short.step(2, 2, { word: 'JOB' });
    beats(s, [
      { at: '#turn', icon: 'sparkles', label: 'The best part', color: 'yellow' },
      { at: '#team', icon: 'users', label: 'Join the staff team', color: 'blue' },
      { at: '#creators', icon: 'video', label: 'Work with creators', color: 'pink' },
    ]);
  },

  apply(s) {
    s.camera(s.short.drift());
    payoff(s, 'Apply now', [
      { text: 'Ticket on store.chickensmp.net', at: '#apply' },
      { text: 'Show your best edits', at: '#edits' },
    ], { icon: 'send' });
    s.image('assets/logo.png', { w: s.safe.w * 0.62, x: s.cx, y: s.safe.y + s.safe.h * 0.1, shadow: false, at: 0.1 });
  },
};
