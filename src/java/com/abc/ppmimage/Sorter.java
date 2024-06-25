package com.abc.ppmimage;

import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo;
import com.abc.thread.ThreadTools;

public class Sorter {
	private final PPDeluxeBoundedFifo<PipelineWork> input;
	private final PPDeluxeBoundedFifo<PipelineWork>[] output;
	private Thread thread;
	private volatile boolean keepGoing;

	public Sorter(PPDeluxeBoundedFifo<PipelineWork> input, PPDeluxeBoundedFifo<PipelineWork>[] output) {
		this.input = input;
		this.output = output;
		keepGoing = true;
		thread = new Thread(this::runWork, getClass().getSimpleName());
		thread.start();
	}

	// takes a stream of mixed rows from multiple images and distributes them to image specific fifos
	private void runWork() {
		try {
			while (keepGoing) {
				PPDeluxeBoundedFifo.RemoveSingleResult<PipelineWork> removeResult = input.remove();
				switch (removeResult.getStatus()) {
					case EMPTY_AND_NO_MORE_ADDS_ALLOWED:
						return;
					case SUCCESS:
						PipelineWork work = removeResult.getItem();
						PPDeluxeBoundedFifo.AddStatus addStatus = output[work.imageIndex].add(work);
						if (addStatus == PPDeluxeBoundedFifo.AddStatus.NO_MORE_ADDS_ALLOWED) {
							throw new IllegalStateException("illegal state");
						}
						break;
				}
			}
		} catch (InterruptedException x) {
			// ignore
		} finally {
			ThreadTools.outln(Thread.currentThread().getName() + " finished");
		}
	}

	public void waitUntilStopped() throws InterruptedException {
		thread.join();
	}

	public void stopRequest() {
		keepGoing = false;
		thread.interrupt();
	}
}
