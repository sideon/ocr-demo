package com.quantum.ocrdemo.controller;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_photo;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.osgi.OpenCVNativeLoader;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.recognition.software.jdeskew.ImageDeskew;
import com.recognition.software.jdeskew.ImageUtil;
import com.sun.scenario.animation.shared.InterpolationInterval;

import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageHelper;

@ManagedBean(name = "ocrDemo")
@SessionScoped
public class OcrDemoBean {
	
	@PostConstruct
	public void init( ) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	private String input;
	private String output;
	
	private String ocrResult;
	
	//output
	private String passType;
	private String employer;
	private String name;
	private String fin;
	private String nationality;
	private String birthdate;
	private String sex;
	
	private Boolean front = Boolean.FALSE;
	
	public Boolean getFront() {
		return front;
	}

	public void setFront(Boolean front) {
		this.front = front;
	}
	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getPassType() {
		return passType;
	}

	public void setPassType(String passType) {
		this.passType = passType;
	}

	public String getEmployer() {
		return employer;
	}

	public void setEmployer(String employer) {
		this.employer = employer;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFin() {
		return fin;
	}

	public void setFin(String fin) {
		this.fin = fin;
	}

	public String getOcrResult() {
		return ocrResult;
	}

	public void setOcrResult(String ocrResult) {
		this.ocrResult = ocrResult;
	}
	 
	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	private static BufferedImage binaryImage(BufferedImage image) {
		return ImageHelper.convertImageToBinary(image);
	}
	
	private static Mat enhanceMat(Mat mat) {
		Mat src = new Mat();
		opencv_imgproc.resize(mat, src, new Size(mat.cols()*2, mat.rows()*2), 0, 0, opencv_imgproc.INTER_CUBIC);
		
		Mat dest = new Mat();
		opencv_photo.fastNlMeansDenoising(src, dest);
		
		Mat dest2 =  new Mat();
		opencv_photo.detailEnhance(src,dest2);
		
		return dest2;
	}
	
	private static BufferedImage deskewImage(BufferedImage image) {
		BufferedImage deskewBufferImage = null;
		ImageDeskew deskewImage = new ImageDeskew(image);
		double skewThreshold = 0.01; 
		double imageSkewAngle = deskewImage.getSkewAngle();
		if (imageSkewAngle > skewThreshold || imageSkewAngle < -skewThreshold) {
			deskewBufferImage = ImageUtil.rotate(image, -imageSkewAngle, image.getWidth() / 2, image.getHeight() / 2);
		} else {
			deskewBufferImage = image;
		}
		return deskewBufferImage;
	}
	
	private static Mat mat(BufferedImage image) {
		Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC(3));
		UByteRawIndexer indexer = mat.createIndexer();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb =image.getRGB(x, y);
				indexer.put(y, x, 0, (rgb >> 0) & 0xFF);
			    indexer.put(y, x, 1, (rgb >> 8) & 0xFF);
			    indexer.put(y, x, 2, (rgb >> 16) & 0xFF);
			}
		}
		indexer.release();
		
		return mat;
	}
	
	public void uploadOcrFile(FileUploadEvent event) {
		UploadedFile ocrFile = event.getFile();
        if (ocrFile != null) {
        	Tesseract instance = new Tesseract();
        	instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        	//instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\data");
            instance.setLanguage("eng");
    	 //   instance.setHocr(true);
            
			try {
				
				BufferedImage image = ImageIO.read(ocrFile.getInputStream());
				showInput(image, ocrFile.getFileName());
				
				BufferedImage binaryImage = binaryImage(image);
				/*
				BufferedImage binaryImage = ImageHelper.convertImageToGrayscale(image);
				binaryImage = ImageHelper.convertImageToBinary(binaryImage);
				*/
				Mat mat = enhanceMat(mat(binaryImage));
				
				BufferedImage matImage = Java2DFrameUtils.toBufferedImage(mat);
				BufferedImage deskewImage = deskewImage(matImage);
				
			    List<Word> words = instance.getWords(deskewImage, TessPageIteratorLevel.RIL_TEXTLINE);
			    System.out.println("Words found: " + words.size());
			    proccessOutput(words);
			    int i = 0;
				for (Word w : words) {
					System.out.println("i: " + (i++));
					System.out.println("Des T: " + w.getText());
					System.out.println("Des C: " + w.getConfidence());
					Rectangle rect = w.getBoundingBox();
					System.out.println(String.format("Box: x=%d, y=%d, w=%d, h=%d", rect.x, rect.y, rect.width, rect.height));
				}
				/*
				saveImage(binaryImage, "C:\\opencv\\images\\output\\bin.jpg");
				saveImage(matImage, "C:\\opencv\\images\\output\\mat.jpg");
				saveImage(deskewImage, "C:\\opencv\\images\\output\\des.jpg");
				*/
				
				showOutput(deskewImage, ocrFile.getFileName());
				ocrResult = instance.doOCR(deskewImage);
				FacesMessage message = new FacesMessage("Successful conversion.");
		        FacesContext.getCurrentInstance().addMessage(null, message);
			} catch (IOException e) {
				FacesMessage message = new FacesMessage("Error in conversion: " + e.getMessage());
		        FacesContext.getCurrentInstance().addMessage(null, message);
			} catch (TesseractException e) {
				FacesMessage message = new FacesMessage("Error in conversion: " + e.getMessage());
		        FacesContext.getCurrentInstance().addMessage(null, message);
			}
        } 
    }
	
	/*
	private static void saveImage(BufferedImage image, String imageFile) throws IOException {
		File outputfile = new File(imageFile);
		if (!outputfile.exists()) {
			outputfile.mkdirs();
			outputfile.createNewFile();
		}
	    ImageIO.write(image, "jpg", outputfile);
	}
	*/
	
	private void showOutput(BufferedImage image, String filename) throws IOException {
		String extension = "png";
		int i = filename.lastIndexOf('.');
		if (i > 0) {
			extension = filename.substring(i + 1).toLowerCase();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, extension, baos);
		String encodedImage = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
		this.output = String.format("data:image/%s;base64, %s", extension, encodedImage);
	}
	
	private void showInput(BufferedImage image, String filename) throws IOException {
		String extension = "png";
		int i = filename.lastIndexOf('.');
		if (i > 0) {
			extension = filename.substring(i + 1).toLowerCase();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, extension, baos);
		String encodedImage = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
		this.input = String.format("data:image/%s;base64, %s", extension, encodedImage);
	}
	
	private void proccessOutput(List<Word> words) {
		if (words.size() == 12) {
			this.front = Boolean.TRUE;
			this.passType = words.get(0).getText().replaceAll("[^\\w\\s]","").trim();
			this.employer = words.get(4).getText().replaceAll("[^\\w\\s]","").trim();
			this.name = words.get(6).getText().replaceAll("[^\\w\\s]","").trim();
			
			String fin = words.get(8).getText();
			Pattern p = Pattern.compile("[STFG]\\d{7}[A-Z]");
		    Matcher m = p.matcher(fin);
		    if (m.find()) {
		    	this.fin = m.group();
		    }
		} else if (words.size() == 16) {
			this.front = Boolean.FALSE;
			this.passType = words.get(0).getText().replaceAll("[^\\w\\s]","").trim();
			this.name = words.get(3).getText().replaceAll("[^\\w\\s]","").trim();
			
			String fin = words.get(6).getText();
			Pattern p = Pattern.compile("[STFG]\\d{7}[A-Z]");
		    Matcher m = p.matcher(fin);
		    if (m.find()) {
		    	this.fin = m.group();
		    }
		    String birthAndSex = words.get(8).getText();
		    p = Pattern.compile("(0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.](19|20)\\d\\d");
		    m = p.matcher(birthAndSex);
		    if (m.find()) {
		    	this.birthdate = m.group();
		    	this.sex = birthAndSex.substring(birthAndSex.indexOf(birthdate) + birthdate.length(), birthAndSex.indexOf(birthdate) + birthdate.length() + 3).trim();
		    }
		    
		    String nationality = words.get(10).getText();
		    p = Pattern.compile("\\b[A-Z]{2,}\\b");
		    m = p.matcher(nationality);
		    if (m.find()) {
		    	this.nationality =  m.group();
		    }
		}
		
	}
	
	public static void main (String[] args) {
		 String s = " eR G6318270U [as ox, a fm)";
		 Pattern p = Pattern.compile("[STFG]\\d{7}[A-Z]");
		    Matcher m = p.matcher(s);
		    if (m.find()) {
		    	System.out.println(m.group());
		    }
		    
		 
	}
	
}

