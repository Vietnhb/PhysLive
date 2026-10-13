import type { SceneDescriptor } from "./sceneModel";
export type TeacherPresentation = {
  version: 1; frameResolver: string;
  fieldRanges: Record<string, { min: number; max: number }>;
  parameters: Record<string, number>;
  initialFrame: { t: number; dt: number; values: Record<string, number> };
  scene: SceneDescriptor;
};
