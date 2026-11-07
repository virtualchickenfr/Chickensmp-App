import { ServerWidget } from './ServerWidget';
import { STATUS_API } from '../content';

const ONLINE_API = 'https://store.chickensmp.net/api/app?action=online';

async function load() {
  const data = { online: false, players: 0, max: 0, names: [], updatedAt: Date.now() };
  try {
    const r = await fetch(STATUS_API);
    const d = await r.json();
    if (d && d.online) {
      data.online = true;
      data.players = d.players ? d.players.online : 0;
      data.max = d.players ? d.players.max : 0;
    }
  } catch (e) { /* offline */ }
  try {
    const r = await fetch(ONLINE_API);
    const d = await r.json();
    data.names = d.names || [];
  } catch (e) { /* ignore */ }
  return data;
}

export async function widgetTaskHandler(props) {
  switch (props.widgetAction) {
    case 'WIDGET_ADDED':
    case 'WIDGET_UPDATE':
    case 'WIDGET_RESIZED':
    case 'WIDGET_CLICK': {
      const data = await load();
      const wide = (props.widgetInfo && props.widgetInfo.width) > 220;
      props.renderWidget(<ServerWidget {...data} wide={wide} />);
      break;
    }
    default:
      break;
  }
}
