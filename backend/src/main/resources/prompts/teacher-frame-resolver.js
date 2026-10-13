function(timeline, cursor) {
  const frames = timeline.frames;
  if (!frames.length) throw Error('Backend timeline has no samples.');
  let lo = 0, hi = frames.length - 1;
  while (lo < hi) {
    const mid = Math.ceil((lo + hi) / 2);
    if (frames[mid].t <= cursor) lo = mid; else hi = mid - 1;
  }
  const sample = frames[lo];
  return {t: sample.t, dt: sample.dt, fields: sample.values};
}
