# JasperReports Demo

A simple **JasperReports demo project** created for demonstration and learning purposes.

The project demonstrates how to generate PDF reports using **JasperReports**, including the use of a **subreport** for displaying related report data.

> **Note:** This project is for **demo purposes only** and is not intended for production use.

---

## Features

* Generate PDF reports using JasperReports
* Main report and subreport integration
* Pass parameters from Java to JasperReports
* Pass data from the main report to a subreport
* Display sample/dummy data
* Calculate report totals
* Export generated reports to PDF
* Demonstrates basic JasperReports integration with Java

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── sandbox/
│   │           └── jasperpdfjava21/
│   │               └── controller/
│   │                   └── TicketReportBean.java
│   │
│   ├── resources/
│   │   └── reports/
│   │       ├── ticket-report.jrxml
│   │       └── ticket-subreport.jrxml
│   │
│   └── webapp/
│       └── pages/
│           └── generateFilters.xhtml
│
└── pom.xml
```

---

## JasperReports

The project uses **JasperReports** to create printable reports from Java data.

The basic report flow is:

```text
Java / JSF
    |
    v
Report Bean
    |
    v
Load JRXML
    |
    v
Compile Report
    |
    v
Pass Parameters / Data
    |
    v
Fill Main Report
    |
    +----> Fill Subreport
    |
    v
JasperPrint
    |
    v
Export to PDF
```

---

## Main Report and Subreport

The demo contains a **main report** and a **subreport**.

### Main Report

The main report is responsible for the overall report layout and header information.

Example:

```text
-----------------------------------------------
              TICKET REPORT
-----------------------------------------------

Ticket No.    Account       Amount
-----------------------------------------------
TCK-1001      60010         150.50
TCK-1002      50020       1,250.00
TCK-1003      40010       3,500.00
-----------------------------------------------

              SUBREPORT
-----------------------------------------------

Description          Amount
-----------------------------------------------
Sample Item 1        100.00
Sample Item 2        200.00
-----------------------------------------------

Total                300.00
```

### Subreport

The subreport is a separate `.jrxml` file that is included inside the main report.

Example:

```text
ticket-report.jrxml
        |
        +---- ticket-subreport.jrxml
```

The main report passes required parameters or data to the subreport.

---

## Subreport Concept

A subreport can be used when a report contains a separate section that has its own layout or data.

For example:

```text
Main Report
│
├── Report Header
│
├── Ticket Information
│
├── Transaction Information
│
└── Subreport
    ├── Detail 1
    ├── Detail 2
    └── Detail 3
```

This makes the report easier to organize and maintain compared with putting everything into a single large JRXML file.

---

## Sample Data

This demo uses sample data such as:

```text
Ticket No.    Account Code    Amount
-------------------------------------
TCK-1001      60010           150.50
TCK-1002      50020         1,250.00
TCK-1003      40010         3,500.00
TCK-1004      60050           750.00
TCK-1005      20010           500.00
```

The data is intended only for demonstration.

---

## Technologies

* Java 21
* JasperReports
* Jakarta Faces / JSF
* PrimeFaces
* Maven
* PDF Export
* JRXML

---

## Maven

JasperReports is added as a Maven dependency.

Example:

```xml
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>YOUR_VERSION</version>
</dependency>
```

Use the JasperReports version configured in the project's `pom.xml`.

---

## Basic JasperReports Process

The typical Java implementation follows these steps:

### 1. Load the JRXML

```java
InputStream reportStream =
        getClass().getResourceAsStream(
                "/reports/ticket-report.jrxml"
        );
```

### 2. Compile the Report

```java
JasperReport jasperReport =
        JasperCompileManager.compileReport(reportStream);
```

### 3. Prepare Parameters

```java
Map<String, Object> parameters = new HashMap<>();

parameters.put("REPORT_TITLE", "Ticket Report");
```

### 4. Fill the Report

```java
JasperPrint jasperPrint =
        JasperFillManager.fillReport(
                jasperReport,
                parameters,
                connection
        );
```

### 5. Export to PDF

```java
JasperExportManager.exportReportToPdfFile(
        jasperPrint,
        "ticket-report.pdf"
);
```

---

## Subreport Parameter

A subreport can receive parameters from the main report.

For example:

```text
Main Report
     |
     | SUBREPORT_DATA
     v
Subreport
```

A parameter can be defined in the main report:

```xml
<parameter
    name="SUBREPORT_DATA"
    class="java.util.Collection"/>
```

The parameter can then be mapped to the subreport.

Example:

```xml
<subreportParameter name="SUBREPORT_DATA">
    <subreportParameterExpression>
        <![CDATA[$P{SUBREPORT_DATA}]]>
    </subreportParameterExpression>
</subreportParameter>
```

---

## Running the Demo

1. Clone or download the project.
2. Open the project in IntelliJ IDEA.
3. Make sure Java 21 is configured.
4. Load the Maven dependencies.
5. Build the project.
6. Run the application.
7. Open the report page.
8. Select the required report option.
9. Enter the demo filters.
10. Generate the report.
11. The report will be generated as a PDF.

---

## Demo Purpose

This project is intended to demonstrate:

* Basic JasperReports integration
* JRXML report design
* Main report and subreport usage
* Parameter passing
* Data handling
* PDF generation
* Integration with a Java web application

This project is **not production-ready** and uses sample data and simplified implementation for demonstration purposes.

---

## Notes

The actual report layout, parameters, database queries, and subreport implementation may change depending on the requirements of the application.

For production implementation, additional considerations should be applied, including:

* Proper database connection management
* Input validation
* Error handling
* Logging
* Security
* Report performance
* Large dataset handling
* Resource management
* Proper report template versioning

---

## License

This project is for **demo and learning purposes only**.
