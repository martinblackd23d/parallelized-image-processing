package com.abc.ppmimage;

import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo;
import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo.AddStatus;
import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo.RemoveSingleResult;
import com.abc.thread.ThreadTools;

public class Worker {
	private static int id = 0;
	private final PPDeluxeBoundedFifo<PipelineWork> input;
	private final PPDeluxeBoundedFifo<PipelineWork> output;
	private Thread thread;
	private volatile boolean keepGoing;
	private PipelineStage stage;

	public Worker(PPDeluxeBoundedFifo<PipelineWork> input, PPDeluxeBoundedFifo<PipelineWork> output, PipelineStage stage) {
		this.input = input;
		this.output = output;
		this.stage = stage;
		keepGoing = true;
		thread = new Thread(this::runWork, getClass().getSimpleName()  + "- " + getNextId() + " stage " + stage);
		thread.start();
	}

	private static synchronized int getNextId() {
		return id++;
	}

	// takes a stream of rows and processes them according to the specified stage
	private void runWork() {
		ThreadTools.outln(Thread.currentThread().getName() + " starting");
		try {
			while (keepGoing) {
				RemoveSingleResult<PipelineWork> removeResult = input.remove();
				switch (removeResult.getStatus()) {
					case EMPTY_AND_NO_MORE_ADDS_ALLOWED:
						return;
					case SUCCESS:
						PipelineWork work = removeResult.getItem();
						work.process(stage);
						AddStatus addStatus = output.add(work);
						if (addStatus == AddStatus.NO_MORE_ADDS_ALLOWED) {
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
