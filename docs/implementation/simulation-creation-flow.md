# Teacher simulation creation ownership

Scope: the teacher creation workspace and its understand, revise, generate, compute, save and reopen requests. Other screens keep their existing behavior.

The backend owns physical parameters, domains, relationships, equations, solver results and validation. FE submits user actions with the original signed simulation contract. It does not clamp edits to a physical domain or infer relationships between independent controls.

Generate, compute and reopen return a presentation contract with effective parameters, field ranges, metadata, initial sample and a server-authored frame resolver. The backend supplies each sample's t and dt. The worker executes that resolver unchanged; teacher readouts and chart markers consume its exact fields. Numeric values and labels are preserved. Missing or unsupported teacher contracts produce an error rather than falling back to FE interpolation. The old shared playback branch remains available to existing screens outside teacher creation.

A parameter edit keeps the previous timeline and effective parameters together until the backend responds. Rendering failures send the original signed contract and the displayed runtime snapshot for diagnostics. An explicit user repair request asks the backend to regenerate the artwork.

FE still handles playback controls, browser timing, pixel coordinates, viewport sizing and chart axes. These presentation operations do not calculate physical state. The approved backend schema continues to determine which models are executable.
