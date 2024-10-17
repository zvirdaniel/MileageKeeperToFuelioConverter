package cz.dazv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class MileageKeeperToFuelioConvertor {
    private static final String OUTPUT_FILE = "Fuelio - ";
    private static final String OUTPUT_EXTENSION = ".csv";

    public enum FuelioFuel {
        GAS_100_OCTANE("100", "114"),
        DIESEL("200", "201"),
        LPG("400", "401"),
        CNG("500", "501");

        public final String tankType;
        public final String fuelType;

        FuelioFuel(String tankType, String fuelType) {
            this.tankType = tankType;
            this.fuelType = fuelType;
        }
    }

    public static void main(String[] args) throws CsvException {
        System.out.println("Please provide the input CSV file path: ");

        // Read file from console
        Scanner scanner = new Scanner(System.in);

        // Check if file exists
        File file = new File(scanner.nextLine());
        if (!file.exists()) {
            System.out.println("File not found");
            return;
        }

        // Read fuel type from console
        String fuelTypes = Arrays.stream(FuelioFuel.values()).map(Enum::name).collect(Collectors.joining(", "));
        System.out.println("Please provide the fuel type (" + fuelTypes + "): ");
        String fuelTypeInput = scanner.nextLine().toUpperCase();
        FuelioFuel fuel;
        try {
            fuel = FuelioFuel.valueOf(fuelTypeInput);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid fuel type. Please provide one of the following: DIESEL, CNG, GASOLINE.");
            return;
        }

        // Read input CSV
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            rows = reader.readAll();
        } catch (IOException e) {
            System.out.println("Cannot read the input file.");
            e.printStackTrace();
            return;
        }

        // Prepare to write to the output CSV
        String fileNameWithoutExtension = file.getName().replaceFirst("[.][^.]+$", "");
        String filename = OUTPUT_FILE + fileNameWithoutExtension + OUTPUT_EXTENSION;
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            // Write the vehicle section (hardcoded as per your example)
            writer.writeNext(new String[]{"## Vehicle"});
            writer.writeNext(new String[]{"Name", "Description", "DistUnit", "FuelUnit", "ConsumptionUnit", "ImportCSVDateFormat", "VIN", "Insurance", "Plate", "Make", "Model", "Year", "TankCount", "Tank1Type", "Tank2Type", "Active", "Tank1Capacity", "Tank2Capacity", "FuelUnitTank2", "FuelConsumptionTank2"});
            writer.writeNext(new String[]{fileNameWithoutExtension, "", "0", "0", "0", "yyyy-MM-dd", "", "", "", "", "", "", "1", fuel.tankType, "0", "1", "0.0", "0.0", "0", "0"});

            // Write the log header
            writer.writeNext(new String[]{"## Log"});
            writer.writeNext(new String[]{"Data", "Odo (km)", "Fuel (litres)", "Full", "Price (optional)", "l/100km (optional)", "latitude (optional)", "longitude (optional)", "City (optional)", "Notes (optional)", "Missed", "TankNumber", "FuelType", "VolumePrice", "StationID (optional)", "ExcludeDistance", "UniqueId", "TankCalc", "Weather"});

            // Iterate over the input rows and process the log entries
            SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            DecimalFormat decimalFormat = new DecimalFormat("#.00");

            boolean firstRow = true;
            for (String[] row : rows) {
                if (row.length == 0 || row[0].isEmpty()) {
                    continue; // Skip empty lines
                }

                if (firstRow) {
                    firstRow = false;
                    continue;
                }

                try {
                    // Transform the input fields to the output format
                    Date date = inputDateFormat.parse(row[0] + " 12:00");
                    String dateConverted = outputDateFormat.format(date); // Convert date format
                    String odometer = row[1] + ".0"; // Odometer as a float
                    String fuelLitres = row[2].replace(",", "."); // Fuel amount, convert comma to dot for decimals
                    String totalCost = row[3].replace(",", "."); // Total cost, convert comma to dot
                    String stationName = row[4]; // Station name
                    String isFullTank = (row[5] == null || row[5].trim().isEmpty()) ? "1" : "0"; // Full or partial tank
                    String isTankSkipped = (row[6] == null || row[6].trim().isEmpty()) ? "0" : "1"; // 1+ tanks skipped before this one
                    double volumePrice = Double.parseDouble(totalCost) / Double.parseDouble(fuelLitres); // Price per litre
                    String volumePriceFormatted = decimalFormat.format(volumePrice);

                    // Write the processed row to the output file
                    writer.writeNext(new String[]{
                            dateConverted,             // Date
                            odometer,                  // Odo (km)
                            fuelLitres,                // Fuel (litres)
                            isFullTank,                // Full
                            totalCost,                 // Price (optional)
                            "0.0",                     // l/100km (optional)
                            "0.0",                     // latitude (optional)
                            "0.0",                     // longitude (optional)
                            "",                        // City (optional)
                            stationName,               // Notes (optional)
                            isTankSkipped,             // Missed
                            "1",                       // TankNumber
                            fuel.fuelType,             // FuelType
                            volumePriceFormatted,      // VolumePrice
                            "0",                       // StationID (optional)
                            "0.0",                     // ExcludeDistance
                            "1",                       // UniqueId
                            "0.0",                     // TankCalc
                            ""                         // Weather
                    });
                } catch (ParseException e) {
                    System.out.println("Error parsing date: " + row[0]);
                    e.printStackTrace();
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot write to the output file.");
            e.printStackTrace();
            return;
        }

        System.out.println("Conversion completed. Output saved to " + filename);
    }
}