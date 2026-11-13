import { FlexWidget, ImageWidget, TextWidget } from 'react-native-android-widget';

const BG = '#1c1c1e';
const time = (ms) => {
  const d = new Date(ms);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

export function ServerWidget({ online, maintenance, players, max, names, updatedAt, wide }) {
  const color = maintenance ? '#f5b82e' : online ? '#3ddc84' : '#ff5a6a';
  return (
    <FlexWidget
      clickAction="OPEN_APP"
      style={{ height: 'match_parent', width: 'match_parent', backgroundColor: BG, borderRadius: 24, padding: 14, flexDirection: 'column', justifyContent: 'space-between' }}
    >
      <FlexWidget style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', width: 'match_parent' }}>
        <ImageWidget image={require('../../assets/logo.png')} imageWidth={86} imageHeight={37} />
        <FlexWidget clickAction="REFRESH" style={{ padding: 6, borderRadius: 12, backgroundColor: '#2a2a2e' }}>
          <TextWidget text="↻" style={{ fontSize: 16, color: '#d0d0d0' }} />
        </FlexWidget>
      </FlexWidget>
      <FlexWidget style={{ flexDirection: 'row', alignItems: 'center' }}>
        <FlexWidget style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: color }} />
        <TextWidget
          text={maintenance ? '  Maintenance' : online ? `  ${players}/${max} online` : '  Offline'}
          style={{ fontSize: 20, color: '#ffffff', fontWeight: 'bold' }}
        />
      </FlexWidget>
      {wide && names && names.length ? (
        <TextWidget text={names.slice(0, 6).join(' · ') + (names.length > 6 ? ` +${names.length - 6}` : '')} maxLines={1} style={{ fontSize: 12, color: '#d0d0d0' }} />
      ) : null}
      <FlexWidget style={{ flexDirection: 'row', justifyContent: 'space-between', width: 'match_parent' }}>
        <TextWidget text="chickensmp.net" style={{ fontSize: 13, color: '#4fb6ff', fontWeight: 'bold' }} />
        <TextWidget text={time(updatedAt)} style={{ fontSize: 11, color: '#9a9a9a' }} />
      </FlexWidget>
    </FlexWidget>
  );
}
