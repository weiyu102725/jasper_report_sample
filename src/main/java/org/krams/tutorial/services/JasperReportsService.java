package org.krams.tutorial.services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.log4j.Logger;

import org.krams.tutorial.model.ReportFilter;

import org.springframework.stereotype.Service;

@Service
public class JasperReportsService {
	protected static Logger logger = Logger.getLogger("JasperReportsService");
	public static final String MEDIA_TYPE_EXCEL = "application/vnd.ms-excel";
	public static final String MEDIA_TYPE_PDF = "application/pdf";
	public static final String EXTENSION_TYPE_EXCEL = "xls";
	public static final String EXTENSION_TYPE_PDF = "pdf";
	private  HashMap<String,String> tokens = new HashMap<String, String>();
	
	public HttpServletResponse export(String type, 
			JasperPrint jp, 
			HttpServletResponse response,
			ByteArrayOutputStream baos,
			String fileName) {
		
		if (type.equalsIgnoreCase(EXTENSION_TYPE_EXCEL)) {
			// Export to output stream
			exportXls(jp, baos);
			 
			// Set our response properties
			// Here you can declare a custom filename
			if (fileName == null) {
				fileName = "Report";
			}
			fileName = fileName +".xls";
			response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
			
			// Set content type
			response.setContentType(MEDIA_TYPE_EXCEL);
			response.setContentLength(baos.size());
			
			return response;
		}
		
		if (type.equalsIgnoreCase(EXTENSION_TYPE_PDF)) {
			// Export to output stream
			exportPdf(jp, baos);
			 
			// Set our response properties
			// Here you can declare a custom filename
			fileName = fileName+ ".pdf";
			response.setHeader("Content-Disposition", "inline; filename=\""+ fileName + "\"");
			
			// Set content type
			response.setContentType(MEDIA_TYPE_PDF);
			response.setContentLength(baos.size());
			
			return response;
			
		} 
		
		throw new RuntimeException("No type set for type " + type);
	}
	
	public void exportXls(JasperPrint jp, ByteArrayOutputStream baos) {
		// Create a JRXlsExporter instance
		JRXlsExporter exporter = new JRXlsExporter();
		 
		// Here we assign the parameters jp and baos to the exporter
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
		 
		// Excel specific parameters
		exporter.setParameter(JRXlsAbstractExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		exporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
		exporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
		
		try {
			exporter.exportReport();
			
		} catch (JRException e) {
			throw new RuntimeException(e);
		}
	}
	
	public HttpServletResponse exportMultipleSheets( 
			List<JasperPrint> jpList, 
			HttpServletResponse response,
			ByteArrayOutputStream baos,
			String fileName) {
		
		// Export to output stream
		exportXlsMultipleSheets(jpList, baos);
		 
		// Set our response properties
		// Here you can declare a custom filename
		fileName = fileName +".xls\"";
		response.setHeader("Content-Disposition", "inline; filename=\"" + fileName);
		
		// Set content type
		response.setContentType(MEDIA_TYPE_EXCEL);
		response.setContentLength(baos.size());
		
		return response;
	}

	public void exportXlsMultipleSheets(List<JasperPrint> jpList, ByteArrayOutputStream baos) {
		// Create a JRXlsExporter instance
		JRXlsExporter exporter = new JRXlsExporter();
		 
		// Here we assign the parameters jp and baos to the exporter
		exporter.setParameter(JRXlsExporterParameter.JASPER_PRINT_LIST, jpList);
		exporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, baos);

		// Excel specific parameters
		//exporter.setParameter(JRXlsAbstractExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		exporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
		exporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
		String[] sheetNames = new String[jpList.size()];
		for (int i = 0; i < sheetNames.length; i++) {
			sheetNames[i] = jpList.get(i).getProperty("sheetName");
		}
		exporter.setParameter(JRXlsAbstractExporterParameter.SHEET_NAMES, sheetNames);
		 
		try {
			exporter.exportReport();
			
		} catch (JRException e) {
			throw new RuntimeException(e);
		}
	}

	public void exportPdf(JasperPrint jp, ByteArrayOutputStream baos) {
		// Create a JRXlsExporter instance
		JRPdfExporter exporter = new JRPdfExporter();
		 
		// Here we assign the parameters jp and baos to the exporter
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
		 
		try {
			exporter.exportReport();
			
		} catch (JRException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void download(ReportFilter filter, String token, JRDataSource jrDataSource, HttpServletResponse response) {
		 
		try {
			JasperPrint jp = getJasperPrint(filter, jrDataSource);
			Object name = filter.getParamsMap().get("reportName");
			if (name != null) {
				jp.setName(name.toString());
			}
			Object sheetName = filter.getParamsMap().get("sheetName");
			if (sheetName != null) {
				jp.setProperty("sheetName", sheetName.toString());
			}
			
			// 6. Create an output byte stream where data will be written
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 
			// 7. Export report
			export(filter.getExportType(), jp, response, baos,filter.getFileName());
			 
			// 8. Write to reponse stream
			write(token, response, baos);
		
		} catch (Exception jre) {
			logger.error("Unable to process download");
			jre.printStackTrace();
		}
	}

	public JasperPrint getJasperPrint(ReportFilter filter,
			JRDataSource jrDataSource) throws JRException {
		JasperPrint jp = null;
		// 1. Add report parameters
		Map<String, Object> params = filter.getParamsMap(); 
		params.put("Title", "User Report");
		boolean isJasperFile = filter.getTemplate().toLowerCase().endsWith(".jasper");
		
		// 2.  Retrieve template
		InputStream reportStream = this.getClass().getResourceAsStream(filter.getTemplate()); 
		 
		JasperReport jr = null;
		if (!isJasperFile) {
			// 3. Convert template to JasperDesign
			JasperDesign jd = JRXmlLoader.load(reportStream);
			
			// 4. Compile design to JasperReport
			jr = JasperCompileManager.compileReport(jd);
		}
		 
		// 5. Create the JasperPrint object
		// Make sure to pass the JasperReport, report parameters, and data source
		//khanhdao: need to develop later
		try {
			if (!isJasperFile) {
				jp = JasperFillManager.fillReport(jr, params, jrDataSource);
			} else {
				jp = JasperFillManager.fillReport(reportStream, params, jrDataSource);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jp;
	}

	public void exportMutilSheets(ReportFilter filter, String token,
			HttpServletResponse response, List<JasperPrint> jpList) {
		// 6. Create an output byte stream where data will be written
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 
		// 7. Export report
		exportMultipleSheets(jpList, response, baos, filter.getFileName());
		 
		// 8. Write to reponse stream
		write(token, response, baos);
	}

	/**
	* Writes the report to the output stream
	*/
	private void write(String token, HttpServletResponse response,
			ByteArrayOutputStream baos) {
		 
		try {
			logger.debug(baos.size());
			
			// Retrieve output stream
			ServletOutputStream outputStream = response.getOutputStream();
			// Write to output stream
			baos.writeTo(outputStream);
			// Flush the stream
			outputStream.flush();
			
			// Remove download token
			remove(token);
			
		} catch (Exception e) {
			logger.error("Unable to write report to the output stream");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	public String check(String token) {
		return tokens.get(token);
	}
	public String generate() {
		String uid = UUID.randomUUID().toString();
		tokens.put(uid, uid);
		return uid;
	}
	
	public void remove(String token) {
		tokens.remove(token);
	}
}
