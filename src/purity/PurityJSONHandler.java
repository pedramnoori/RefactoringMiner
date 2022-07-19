package purity;

//import org.json.simple.*;
//import org.json.simple.parser.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PurityJSONHandler {

    public static void main(String[] args) {

//        addPurityFields("C:\\Users\\Pedram\\Desktop\\data.json", "C:\\Users\\Pedram\\Desktop\\Puritydata.json");
//        addPurityFields("C:\\Users\\Pedram\\Desktop\\datatest.json", "C:\\Users\\Pedram\\Desktop\\PurityTestdata.json");

        runPurityOnSpecificRefactoringOperation("C:\\Users\\Pedram\\Desktop\\PurityTestdata.json", RefactoringType.EXTRACT_OPERATION);

    }

    private static void runPurityOnSpecificRefactoringOperation(String sourcePath, RefactoringType refactoringType) {

        ObjectMapper objectMapper = new ObjectMapper();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        File file = new File(sourcePath);
        try {
            JsonNode root = objectMapper.readTree(file);

            for (JsonNode jsonNode : root) {
                miner.detectModelDiff(jsonNode.get("repository").textValue(),
                        jsonNode.get("sha1").textValue(), new RefactoringHandler() {
                            @Override
                            public void processModelDiff(String commitId, UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
                                Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(umlModelDiff);

                                for (JsonNode refactoring : jsonNode.get("refactorings")) {
                                    if (refactoring.get("validation").textValue().equals("TP") || refactoring.get("validation").textValue().equals("FN")) {
                                        for (Map.Entry<Refactoring, PurityCheckResult> entry : pcr.entrySet()) {
                                            if (entry.getValue() != null) {
                                                if (entry.getKey().toString().replaceAll("\\s+", "").equals(refactoring.get("description").textValue().replaceAll("\\s+", ""))) {
                                                    ObjectNode objectNode = (ObjectNode) refactoring;
                                                    if (entry.getValue().isPure() && refactoring.get("purity").textValue().equals("1")) {
                                                        objectNode.put("purityValidation", "TP");
                                                    } else if (!entry.getValue().isPure() && refactoring.get("purity").textValue().equals("0")) {
                                                        objectNode.put("purityValidation", "TN");
                                                    } else if (entry.getValue().isPure() && refactoring.get("purity").textValue().equals("0")) {
                                                        objectNode.put("purityValidation", "FP");
                                                    } else if (!entry.getValue().isPure() && refactoring.get("purity").textValue().equals("1")) {
                                                        objectNode.put("purityValidation", "FN");
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                arrayNode.add(jsonNode);
                            }
                        }, 100);
            }
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File("C:\\Users\\Pedram\\Desktop\\TestRes.json"), arrayNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addPurityFields(String sourcePath, String destPath) {

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        try {
            File file = new File(sourcePath);
            JsonNode root = objectMapper.readTree(file);

            for (JsonNode jsonNode: root) {

                for (JsonNode refactoring: jsonNode.get("refactorings")) {
                    ObjectNode objectNode = (ObjectNode) refactoring;
                    objectNode.put("purity", "-");
                    objectNode.put("purityValidation", "-");
                    objectNode.put("purityComment", "");
                }
                arrayNode.add(jsonNode);
            }

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File(destPath), arrayNode);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
