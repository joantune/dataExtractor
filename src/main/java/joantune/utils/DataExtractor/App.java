package joantune.utils.DataExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.google.common.collect.LinkedHashMultimap;

public class App {

    public static void main(String[] args) {
//        System.out.println("Arguments size" + args.length + " Arguments: " + Arrays.toString(args));
        Options options = new Options();
        //options.addOption("o", true, "name/path of the output CSV file");
        //options.addOption("i", true, "name/path of the optional intermediate pdf file with the relevant data");
        options.addOption("f", true, "name/path of the input TXT file");

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            System.err.println("Error parsing command arguments" + e.getMessage());
        }
        if (cmd.hasOption("f") == false) {
            HelpFormatter formatter = new HelpFormatter();
            System.out.println("You need to specify an f flag");
            formatter.printHelp("DataExtractor", options);
        } else {
            try {

                execute(cmd);
            } catch (IOException io) {
                System.err.println("Caught an error. " + io.getMessage());
            }
        }

    }

    private static void execute(CommandLine cmd) throws IOException {
        //let's read the PDF file
        String filePath = cmd.getOptionValue("f");
//        System.out.println("Reading file: " + filePath);
        File file = new File(filePath);
        if (file.exists() == false) {
            System.err.println("Given file: " + filePath + " does not exist");
        }
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        LinkedHashSet<String> columnNames = new LinkedHashSet<String>();
        try(           CSVPrinter csvPrinter = new CSVPrinter(System.out, CSVFormat.DEFAULT)) {
 
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                String processedLine = StringUtils.strip(line, "{}.");
                String[] splittedString = StringUtils.split(processedLine, ",");
                for (String dataPair : splittedString) {
                    String[] pair = StringUtils.split(dataPair, ":");
                    if(pair.length == 2) {
                        String columnName = StringUtils.strip(pair[0], "\"");
                        columnNames.add(columnName);
                    }

                }
            }

            bufferedReader.close();
            fileReader.close();
            //we have all of the column names, let's reiterate
            LinkedHashMultimap<String, String> finalData = LinkedHashMultimap.create();
            
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            
            for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                String processedLine = StringUtils.strip(line, ".");
                if (StringUtils.isBlank(processedLine)) {
                    continue;
                }
                JSONObject object = new JSONObject(processedLine);
                String[] listColumns = JSONObject.getNames(object);
                for (String columnName : listColumns) {
                    if (StringUtils.equals(columnName, "answers") == false) {
                        columnNames.add(columnName);
                    } else {
                        JSONObject answersObject = object.getJSONObject("answers");
                        String[] answers = JSONObject.getNames(answersObject);
                        for (String answerNr : answers) {
                            columnNames.add(answerNr);
                        }
                    }
                }
                
                
            }

            //let's write the column names
            csvPrinter.printRecord(columnNames);

            //we have all the column names, let's re-read the file and write it to CSV
            bufferedReader.close();
            fileReader.close();

            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                String processedLine = StringUtils.strip(line, ".");
                if (StringUtils.isBlank(processedLine)) {
                    continue;
                }
                JSONObject object = new JSONObject(processedLine);
                List<String> jsonKeys = Arrays.asList(JSONObject.getNames(object));
                JSONObject answers = new JSONObject();
                if (jsonKeys.contains("answers")) {
                    answers = object.getJSONObject("answers");
                }
                for (String columnName : columnNames) {
                    String answer = answers.optString(columnName, null);
                    if (jsonKeys.contains(columnName) || answer != null) {
                        if (jsonKeys.contains(columnName)) {
                            csvPrinter.print(object.get(columnName));
                        } else {
                            csvPrinter.print(answer);
                        }
                    } else {
                        csvPrinter.print("");
                    }
                }

                csvPrinter.println();
            }

        } catch (IOException e) {
            System.err.println("Problem while reading file." + e.getMessage());
            return;
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
            System.out.flush();
        }


    }



}
