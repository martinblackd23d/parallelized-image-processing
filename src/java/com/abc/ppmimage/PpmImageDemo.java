package com.abc.ppmimage;

import java.io.*;

import com.abc.thread.*;

import com.abc.pp.fifo.deluxe_bounded.*;
import com.abc.pp.fifo.deluxe_bounded.impl.*;

public class PpmImageDemo {

    public static PpmImage[] executePipeline(PpmImage[] images, PipelineStage[] pipeline, int workersPerStage) throws InterruptedException{
        PpmImage[] newImages = new PpmImage[images.length];
        //workersPerStage = 4;
        //int fifoSize = workersPerStage * 10;
        int fifoSize = 50;

        // create fifos
        PPDeluxeBoundedFifo<PipelineWork>[] pipelineFifos = new CircularArrayPPDeluxeBoundedFifo[pipeline.length + 1];
        for (int i = 0; i < pipelineFifos.length; i++) {
            pipelineFifos[i] = new CircularArrayPPDeluxeBoundedFifo<>(fifoSize, PipelineWork.class);
        }

        // create 1 producer per image
        Producer[] producers = new Producer[images.length];
        for (int i = 0; i < images.length; i++) {
            producers[i] = new Producer(images[i], i, pipelineFifos[0]);
        }

        // create workers per stage
        Worker[][] workers = new Worker[pipeline.length][workersPerStage];
        for (int i = 0; i < pipeline.length; i++) {
            for (int j = 0; j < workersPerStage; j++) {
                workers[i][j] = new Worker(pipelineFifos[i], pipelineFifos[i + 1], pipeline[i]);
            }
        }

        // create 1 fifo per image for the unsorted rows
        PPDeluxeBoundedFifo<PipelineWork>[] processedRows = new CircularArrayPPDeluxeBoundedFifo[images.length];
        for (int i = 0; i < images.length; i++) {
            processedRows[i] = new CircularArrayPPDeluxeBoundedFifo<>(fifoSize, PipelineWork.class);
        }

        // create sorter that distributes the mixed rows to the appropriate image assembler
        // only 1 sorter is implemented here, but more could be added
        Sorter sorter = new Sorter(pipelineFifos[pipelineFifos.length - 1], processedRows);

        // create 1 assembler per image
        Assembler[] assemblers = new Assembler[images.length];
        for (int i = 0; i < images.length; i++) {
            assemblers[i] = new Assembler(processedRows[i], newImages, i, images[i].getRowCount());
        }

        // wait for all producers to finish
        for (int i = 0; i < producers.length; i++) {
            producers[i].waitUntilStopped();
        }
        pipelineFifos[0].indicateNoMoreAddsAllowed();

        // wait for all workers to finish
        for (int i = 0; i < workers.length; i++) {
            // wait for stage to finish
            for (int j = 0; j < workers[i].length; j++) {
                workers[i][j].waitUntilStopped();
            }
            pipelineFifos[i + 1].indicateNoMoreAddsAllowed();
        }

        // wait for sorter to finish
        sorter.waitUntilStopped();
        for (int i = 0; i < assemblers.length; i++) {
            processedRows[i].indicateNoMoreAddsAllowed();
        }

        // wait for all assemblers to finish
        for (int i = 0; i < assemblers.length; i++) {
            assemblers[i].waitUntilStopped();
        }

        return newImages;
    }

    public static PpmImage flipHorizontally(PpmImage imageOriginal) {
        PpmImage.Row[] newRows = new PpmImage.Row[imageOriginal.getRowCount()];
        for (int rowIndex = 0; rowIndex < newRows.length; rowIndex++) {
            newRows[rowIndex] = imageOriginal.getRowAt(rowIndex).asRowFlippedHorizontally();
        }
        return new PpmImage(newRows);
    }

    public static PpmImage grayscale(PpmImage imageOriginal) {
        PpmImage.Row[] newRows = new PpmImage.Row[imageOriginal.getRowCount()];
        for (int rowIndex = 0; rowIndex < newRows.length; rowIndex++) {
            PpmImage.Pixel[] newPixelsForRow = new PpmImage.Pixel[imageOriginal.getColumnCount()];
            for (int colIndex = 0; colIndex < newPixelsForRow.length; colIndex++) {
                newPixelsForRow[colIndex] = imageOriginal.getPixelAt(rowIndex, colIndex).asGrayscale();
            }
            newRows[rowIndex] = new PpmImage.Row(newPixelsForRow);
        }
        return new PpmImage(newRows);
    }

    public static void mainSerial(String[] args) {
        NanoTimer timer = NanoTimer.createStarted();
        NanoTimer ioTimer = NanoTimer.createStopped();
        NanoTimer imageProcessingTimer = NanoTimer.createStopped();
        try {
            ioTimer.start();
            ThreadTools.outln("reading penguin...");
            PpmImage penguinOriginal = PpmImage.createFromFilename("src/images/penguin.ppm");
            ThreadTools.outln("reading flowers...");
            PpmImage flowersOriginal = PpmImage.createFromFilename("src/images/flowers.ppm");
            ioTimer.stop();

            imageProcessingTimer.start();
            ThreadTools.outln("processing penquin - flipping");
            PpmImage penguinFlippedHorizontally = flipHorizontally(penguinOriginal);
            ThreadTools.outln("processing penquin - grayscaling");
            PpmImage penguinFlippedAndGrayscaled = grayscale(penguinFlippedHorizontally);

            ThreadTools.outln("processing flowers - flipping");
            PpmImage flowersFlippedHorizontally = flipHorizontally(flowersOriginal);
            ThreadTools.outln("processing flowers - grayscaling");
            PpmImage flowersFlippedAndGrayscaled = grayscale(flowersFlippedHorizontally);
            imageProcessingTimer.stop();

            ioTimer.start();
            ThreadTools.outln("writing penguin images");
            penguinFlippedHorizontally.writeToFilename("src/images/penguin-flipped-horiz.ppm");
            penguinFlippedAndGrayscaled.writeToFilename("src/images/penguin-flipped-horiz-and-grayscaled.ppm");

            ThreadTools.outln("writing flowers images");
            flowersFlippedHorizontally.writeToFilename("src/images/flowers-flipped-horiz.ppm");
            flowersFlippedAndGrayscaled.writeToFilename("src/images/flowers-flipped-horiz-and-grayscaled.ppm");
            ioTimer.stop();
        } catch (FileNotFoundException x) {
            x.printStackTrace();
        } catch (IOException x) {
            x.printStackTrace();
        } finally {
            timer.stop();
            ThreadTools.outln("finished processing images, overall took %.5fs, %.5fs I/O, %.5fs image manipulation",
                timer.getElapsedSeconds(), ioTimer.getElapsedSeconds(), imageProcessingTimer.getElapsedSeconds());
        }
    }

    public static void mainParallel(String[] args) throws InterruptedException {
        NanoTimer timer = NanoTimer.createStarted();
        NanoTimer ioTimer = NanoTimer.createStopped();
        NanoTimer imageProcessingTimer = NanoTimer.createStopped();
        try {
            ioTimer.start();
            ThreadTools.outln("reading penguin...");
            PpmImage penguinOriginal = PpmImage.createFromFilename("src/images/penguin.ppm");
            ThreadTools.outln("reading flowers...");
            PpmImage flowersOriginal = PpmImage.createFromFilename("src/images/flowers.ppm");
            ioTimer.stop();

            imageProcessingTimer.start();
            PpmImage[] images = { penguinOriginal, flowersOriginal };
            PipelineStage[] pipeline = { PipelineStage.FLIP_HORIZONTALLY, PipelineStage.GRAYSCALE};
            ThreadTools.outln("processing images in pipeline");
            images = executePipeline(images, pipeline, 4);
            PpmImage penguinFlippedAndGrayscaled = images[0];
            PpmImage flowersFlippedAndGrayscaled = images[1];
            imageProcessingTimer.stop();

            ioTimer.start();
            ThreadTools.outln("writing penguin images");
            //penguinFlippedHorizontally.writeToFilename("src/images/penguin-flipped-horiz.ppm");
            penguinFlippedAndGrayscaled.writeToFilename("src/images/penguin-flipped-horiz-and-grayscaled.ppm");

            ThreadTools.outln("writing flowers images");
            //flowersFlippedHorizontally.writeToFilename("src/images/flowers-flipped-horiz.ppm");
            flowersFlippedAndGrayscaled.writeToFilename("src/images/flowers-flipped-horiz-and-grayscaled.ppm");
            ioTimer.stop();
        } catch (FileNotFoundException x) {
            x.printStackTrace();
        } catch (IOException x) {
            x.printStackTrace();
        } finally {
            timer.stop();
            ThreadTools.outln("finished processing images, overall took %.5fs, %.5fs I/O, %.5fs image manipulation",
                timer.getElapsedSeconds(), ioTimer.getElapsedSeconds(), imageProcessingTimer.getElapsedSeconds());
        }
    }

    public static void timeSerial() {
        NanoTimer timer = NanoTimer.createStarted();
        NanoTimer ioTimer = NanoTimer.createStopped();
        NanoTimer imageProcessingTimer = NanoTimer.createStopped();
        try {
            ioTimer.start();
            ThreadTools.outln("reading penguin...");
            PpmImage penguinOriginal = PpmImage.createFromFilename("src/images/penguin.ppm");
            ThreadTools.outln("reading flowers...");
            PpmImage flowersOriginal = PpmImage.createFromFilename("src/images/flowers.ppm");
            ioTimer.stop();

            PpmImage penguinFlippedHorizontally = null;
            PpmImage penguinFlippedAndGrayscaled = null;
            PpmImage flowersFlippedHorizontally = null;
            PpmImage flowersFlippedAndGrayscaled = null;

            ThreadTools.outln("starting averaging");
            Double total = 0.0;
            for (int i = 0; i < 20; i++) {
                imageProcessingTimer = NanoTimer.createStopped();
                imageProcessingTimer.start();
                ThreadTools.outln("processing penquin - flipping");
                penguinFlippedHorizontally = flipHorizontally(penguinOriginal);
                ThreadTools.outln("processing penquin - grayscaling");
                penguinFlippedAndGrayscaled = grayscale(penguinFlippedHorizontally);

                ThreadTools.outln("processing flowers - flipping");
                flowersFlippedHorizontally = flipHorizontally(flowersOriginal);
                ThreadTools.outln("processing flowers - grayscaling");
                flowersFlippedAndGrayscaled = grayscale(flowersFlippedHorizontally);
                imageProcessingTimer.stop();
                total += imageProcessingTimer.getElapsedSeconds();
            }
            ThreadTools.outln("Average serialized processing time: " + total/20);
            ThreadTools.outln("finished averaging");

            ioTimer.start();
            ThreadTools.outln("writing penguin images");
            penguinFlippedHorizontally.writeToFilename("src/images/penguin-flipped-horiz.ppm");
            penguinFlippedAndGrayscaled.writeToFilename("src/images/penguin-flipped-horiz-and-grayscaled.ppm");

            ThreadTools.outln("writing flowers images");
            flowersFlippedHorizontally.writeToFilename("src/images/flowers-flipped-horiz.ppm");
            flowersFlippedAndGrayscaled.writeToFilename("src/images/flowers-flipped-horiz-and-grayscaled.ppm");
            ioTimer.stop();
        } catch (FileNotFoundException x) {
            x.printStackTrace();
        } catch (IOException x) {
            x.printStackTrace();
        } finally {
            timer.stop();
            ThreadTools.outln("finished processing images, overall took %.5fs, %.5fs I/O, %.5fs image manipulation",
                timer.getElapsedSeconds(), ioTimer.getElapsedSeconds(), imageProcessingTimer.getElapsedSeconds());
        }
    }

    public static void timeParallel() throws InterruptedException {
        NanoTimer timer = NanoTimer.createStarted();
        NanoTimer ioTimer = NanoTimer.createStopped();
        NanoTimer imageProcessingTimer = NanoTimer.createStopped();
        try {
            ioTimer.start();
            ThreadTools.outln("reading penguin...");
            PpmImage penguinOriginal = PpmImage.createFromFilename("src/images/penguin.ppm");
            ThreadTools.outln("reading flowers...");
            PpmImage flowersOriginal = PpmImage.createFromFilename("src/images/flowers.ppm");
            ioTimer.stop();

            PpmImage penguinFlippedAndGrayscaled = null;
            PpmImage flowersFlippedAndGrayscaled = null;
            Double total = 0.0;
            ThreadTools.outln("starting averaging");
            for (int i = 0; i < 20; i++) {
                imageProcessingTimer = NanoTimer.createStopped();
                imageProcessingTimer.start();
                PpmImage[] images = { penguinOriginal, flowersOriginal };
                PipelineStage[] pipeline = { PipelineStage.FLIP_HORIZONTALLY, PipelineStage.GRAYSCALE};
                ThreadTools.outln("processing images in pipeline");
                images = executePipeline(images, pipeline, 4);
                penguinFlippedAndGrayscaled = images[0];
                flowersFlippedAndGrayscaled = images[1];
                imageProcessingTimer.stop();
                total += imageProcessingTimer.getElapsedSeconds();
            }
            ThreadTools.outln("Average parallel processing time: " + total/20);
            ThreadTools.outln("finished averaging");

            ioTimer.start();
            ThreadTools.outln("writing penguin images");
            //penguinFlippedHorizontally.writeToFilename("src/images/penguin-flipped-horiz.ppm");
            penguinFlippedAndGrayscaled.writeToFilename("src/images/penguin-flipped-horiz-and-grayscaled.ppm");

            ThreadTools.outln("writing flowers images");
            //flowersFlippedHorizontally.writeToFilename("src/images/flowers-flipped-horiz.ppm");
            flowersFlippedAndGrayscaled.writeToFilename("src/images/flowers-flipped-horiz-and-grayscaled.ppm");
            ioTimer.stop();
        } catch (FileNotFoundException x) {
            x.printStackTrace();
        } catch (IOException x) {
            x.printStackTrace();
        } finally {
            timer.stop();
            ThreadTools.outln("finished processing images, overall took %.5fs, %.5fs I/O, %.5fs image manipulation",
                timer.getElapsedSeconds(), ioTimer.getElapsedSeconds(), imageProcessingTimer.getElapsedSeconds());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        mainParallel(args);
        //mainSerial(args);
        //timeSerial();
        //timeParallel();
    }
}