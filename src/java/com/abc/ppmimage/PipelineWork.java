package com.abc.ppmimage;

enum PipelineStage {
	FLIP_HORIZONTALLY, GRAYSCALE
}

public class PipelineWork {
	public PpmImage.Row row;
	public final int rowIndex;
	public final int imageIndex;

	public PipelineWork(PpmImage.Row row, int rowIndex, int imageIndex) {
		this.row = row;
		this.rowIndex = rowIndex;
		this.imageIndex = imageIndex;
	}

	public void process(PipelineStage stage) {
		switch (stage) {
			case FLIP_HORIZONTALLY:
				row = row.asRowFlippedHorizontally();
				break;
			case GRAYSCALE:
				toGrayscale();
				break;
			default:
				throw new IllegalArgumentException("unknown stage: " + stage);
		}
	}

	private void toGrayscale() {
		PpmImage.Pixel[] newPixels = new PpmImage.Pixel[row.getColumnCount()];
		for (int i = 0; i < newPixels.length; i++) {
			PpmImage.Pixel pixel = row.getPixelAt(i);
			newPixels[i] = pixel.asGrayscale();
		}
		row = new PpmImage.Row(newPixels);
	}
}
