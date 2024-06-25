package com.abc.ppmimage;

import com.abc.pp.fifo.deluxe_bounded.PPDeluxeBoundedFifo;
import com.abc.thread.ThreadTools;

public class Producer {
	private final PPDeluxeBoundedFifo<PipelineWork> output;
	private Thread thread;
	private volatile boolean keepGoing;
	private PpmImage image;
	private int imageIndex;

	public Producer(PpmImage image, int imageIndex, PPDeluxeBoundedFifo<PipelineWork> output) {
		this.output = output;
		this.image = image;
		this.imageIndex = imageIndex;
		keepGoing = true;
		thread = new Thread(this::runWork, getClass().getSimpleName() + "- image " + imageIndex);
		thread.start();
	}

	// takes an input image and creates a stream of rows
	private void runWork() {
		ThreadTools.outln(Thread.currentThread().getName() + " starting");
		try {
			for (int i = 0; i < image.getRowCount() && keepGoing; i++) {
				output.add(new PipelineWork(image.getRowAt(i), i, imageIndex));
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
