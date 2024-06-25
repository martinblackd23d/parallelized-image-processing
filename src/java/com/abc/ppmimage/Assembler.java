package com.abc.ppmimage;

import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo;
import com.abc.thread.ThreadTools;

public class Assembler {
	private final PPDeluxeBoundedFifo<PipelineWork> input;
	private final PpmImage[] output;
	private final PpmImage.Row[] rows;
	private final int imageIndex;
	private Thread thread;
	private volatile boolean keepGoing;

	public Assembler(PPDeluxeBoundedFifo<PipelineWork> input, PpmImage[] output, int imageIndex, int rowCount) {
		this.input = input;
		this.output = output;
		this.imageIndex = imageIndex;
		rows = new PpmImage.Row[rowCount];
		keepGoing = true;
		thread = new Thread(this::runWork, getClass().getSimpleName() + "- image " + imageIndex);
		thread.start();
	}

	// takes a stream of rows and assembles them into an image
	private void runWork() {
		try {
			while (keepGoing) {
				PPDeluxeBoundedFifo.RemoveSingleResult<PipelineWork> removeResult = input.remove();
				switch (removeResult.getStatus()) {
					case EMPTY_AND_NO_MORE_ADDS_ALLOWED:
						return;
					case SUCCESS:
						PipelineWork work = removeResult.getItem();
						rows[work.rowIndex] = work.row;
						break;
				}
			}
		} catch (InterruptedException x) {
			// ignore
		} finally {
			output[imageIndex] = new PpmImage(rows);
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
