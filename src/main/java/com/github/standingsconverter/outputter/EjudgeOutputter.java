package com.github.standingsconverter.outputter;

import com.github.standingsconverter.entity.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class EjudgeOutputter implements Outputter {
	@Override
	public void output(Contest contest, String outputFile) throws IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.newDocument();
			Element runLogElement = document.createElement("runlog");
			runLogElement.setAttribute("duration", Long.toString(contest.getDuration() * 60));
            runLogElement.setAttribute("fog_time", "3600"); // TODO needed for unfreezing by OSt's script
			document.appendChild(runLogElement);
			Element contestNameElement = document.createElement("name");
			contestNameElement.setTextContent(contest.getName());
			runLogElement.appendChild(contestNameElement);
			Element usersElement = document.createElement("users");
			runLogElement.appendChild(usersElement);
			for (Team team : contest.getTeams()) {
				Element userElement = document.createElement("user");
				userElement.setAttribute("id", Integer.toString(team.getId()));
				userElement.setAttribute("name", team.getName());
				usersElement.appendChild(userElement);
			}
			Element problemsElement = document.createElement("problems");
			runLogElement.appendChild(problemsElement);
			for (Problem problem : contest.getProblems()) {
				Element problemElement = document.createElement("problem");
				problemElement.setAttribute("id", Integer.toString(problem.getId() - 'A' + 1));
				problemElement.setAttribute("short_name", Character.toString(problem.getId()));
                problemElement.setAttribute("long_name", problem.getName());
				problemsElement.appendChild(problemElement);
			}
			Element runsElement = document.createElement("runs");
			runLogElement.appendChild(runsElement);
			int runID = 0;
			for (Submission submission : contest.getSubmissions()) {
				Element runElement = document.createElement("run");
				runElement.setAttribute("run_id", Integer.toString(runID++));
				runElement.setAttribute("time", Long.toString(submission.getTime()));
				runElement.setAttribute("status", toString(submission.getVerdict()));
				runElement.setAttribute("user_id", Integer.toString(submission.getTeam().getId()));
				runElement.setAttribute("prob_id", Integer.toString(submission.getProblem().getId() - 'A' + 1));
				runsElement.appendChild(runElement);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
            DOMSource source = new DOMSource(document);
			StreamResult stream = new StreamResult(new File(outputFile));
			transformer.transform(source, stream);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	private String toString(Verdict verdict) {
		// TODO Ejudge doesn't support IL, it is replaced with TL
		switch (verdict) {
			case ACCEPTED: return "OK";
			case REJECTED: return "RJ";
			case WRONG_ANSWER: return "WA";
			case RUNTIME_ERROR: return "RT";
			case TIME_LIMIT_EXCEEDED: return "TL";
			case MEMORY_LIMIT_EXCEEDED: return "ML";
			case COMPILATION_ERROR: return "CE";
			case PRESENTATION_ERROR: return "PE";
			case IDLENESS_LIMIT_EXCEEDED: return "TL";
			case SECURITY_VIOLATION: return "SE";
		}
		throw new IllegalArgumentException("Unknown verdict: " + verdict);
	}
}
