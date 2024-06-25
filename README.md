## What is the average time for twenty runs of the serial version of the code?
3.5s of processing time with a SLOW_FACTOR of 250

## What is the average time for twenty runs of the parallel version of the code?
2.2s of processing time with a SLOW_FACTOR of 250 and 2+ workers per pipeline stage. With 1 worker per stage, it's 2.6s.


## Calculate the speedup of the parallel version. Is the parallel code significantly faster?
The parallel version is ~40% faster. This holds even for larger slow factors as well, where the overhead from the parallelization is minimal. With 1 worker per stage (so 2 workers total), the speed was slightly lower, and for 2 and more workers per stage (4 or more in total), the speed remained the same.

## The Methodology section above described how you decompose the image processing routines to parallelize them.
The parallelized code uses the following pipeline. Each stage runs in parallel, with 1 shared fifo in between each step, except for the last.

1. Images are broken down into rows.
  - The images are processed separately, with 1 producer per image, and the rows go into a shared pool to be processed.

2. The individual rows are flipped.
  - The number of workers working in parallel is adjustable.

3. The individual rows are grayscaled.
  - The number of workers is adjustable.

4. The rows are sorted into different fifos depending on which image they belong to.
  - Only one sorter works at any given time, although more could be added.

5. The final images are reassembled from the rows.
  - One assembler works on any given image.

## Potential changes:
- Because they are not affected by the slow factor, the efficiency gains or losses from parallelizing breaking down and reassembling multiple images is hard to evaluate, and the pipeline works perfectly even without them. With PPMs and with only 2 images, it likely doesn't matter, but with more computationally expensive encodings, such as compressed images, it might be significant.
- More sorters could be added, although since it doesn't really do any processing, it likely doesn't matter.
- Here, the stages are clearly separated, with separate input and output fifos in between, but the processing is defined in the PipelineWork classes, and the workers are identical, so depending on new requirements it could be modified in 2 directions.
  - The pipeline stage could be stored in the PipelineWork class as well, workers could always process the next stage and the processed objects could be placed back into the input queue until they are done. This way, instead of having separate pools of workers for each stage, there's only one shared pool of workers, theoretically making the system more efficient.
  - The workers could be differentiated, and depending on the needs, stages could even operate at different granularities, for example on row or pixel level, depending on the needs.


