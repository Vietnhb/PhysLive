export type Version = {
  id: string;
  schemaId: string;
  version: string;
  lifecycleStatus: string;
  name?: string;
  topic?: string;
  definition?: unknown;
  definitionChecksum?: string;
  recordVersion?: number;
  createdAt: string;
};

export type ModuleRelease = {
  id: string;
  topic: string;
  moduleName: string;
  schemaId: string;
  schemaVersion: string;
  lifecycleStatus: "DRAFT" | "APPROVED" | "RETIRED";
};
