package com.multispring.initializr.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.StandardOpenOption;

public class SpringBootProjectInitializer {
    public static void createSpringBootProject(String projectName, String javaVersion,
                                               List<String> addOns, String database, HttpServletResponse response) throws IOException, InterruptedException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                projectName+".zip");
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath = Paths.get(projectName);
        Files.createDirectories(srcPath);
        createParentPom(projectName,srcPath,javaVersion,addOns,database);
        createDockerBuildProperties(projectName,srcPath);
        createAndWritePomFile(srcPath, "Jenkinsfile", "/src/main/resources/static/parent/Jenkinsfile", projectName);
        Path dockerFile = srcPath.resolve("Dockerfile");
        String dockerFileContent ="FROM asia.gcr.io/nonprod-utility-233414/base-images/springboot-java11:springboot-java11-alpine-1.0.6\n" +
                "MAINTAINER SUTHIR S";
        Files.createFile(dockerFile);
        Files.write(dockerFile,dockerFileContent.getBytes());
        createWebProject(projectName,srcPath,addOns,database);
        createModelProject(projectName,srcPath,database,addOns);
        createServiceProject(projectName,srcPath,addOns);
        createServiceImplProject(projectName,srcPath, addOns,database);
        createRepositoryProject(projectName,srcPath,database);
        createOutboundProject(projectName,srcPath);
        createConfigurationProject(projectName,srcPath);
        createCoverageProject(projectName,srcPath);
        zipSpringBootProject(projectName+"/", response.getOutputStream());
    }
    private static void createParentPom(String projectName,Path srcPath, String javaVersion,
                                        List<String> addOns , String database) throws IOException {
        Path pomFile = srcPath.resolve("pom.xml");
        String pomContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/pom.xml");
        if (addOns != null) {
            if (addOns.contains("kafka")) {
                pomContent = addKafkaDependency(pomContent);
            } if (database!=null && database.equalsIgnoreCase("postgres")) {
                pomContent = addPostgresDependency(pomContent);
            } if (database!=null && database.equalsIgnoreCase("mongo")) {
                pomContent = addMongoDependency(pomContent);
            }
        }
        if(!javaVersion.isBlank() && javaVersion.equals("11")){
            final String springbootVersionForJava11 = "2.7.8";
            pomContent = pomContent.replace("{springbootVersion}",springbootVersionForJava11);
            Files.write(pomFile, pomContent.getBytes());
        } else if (!javaVersion.isBlank() && javaVersion.equals("17")) {
            final String springbootVersionForJava17 = "3.1.1";
            pomContent = pomContent.replace("{springbootVersion}",springbootVersionForJava17);
            Files.write(pomFile, pomContent.getBytes());
        }
        String modifiedContent = pomContent.replace("${placeholder}", javaVersion).replace("{artifactId}", projectName).replace("{groupId}","com.multispring."+projectName.replace("-", "."));
        Files.write(pomFile, modifiedContent.getBytes());
    }

    private static String addKafkaDependency(String content) {
        String kafkaDependency = """
                    \t<dependency>
                    \t\t\t<groupId>org.springframework.kafka</groupId>
                    \t\t\t<artifactId>spring-kafka</artifactId>
                    \t\t</dependency>\n""";
        int dependenciesEndIndex = content.indexOf("</dependencies>");
        if (dependenciesEndIndex != -1) {
            content = content.substring(0, dependenciesEndIndex) + kafkaDependency + content.substring(dependenciesEndIndex);
        }
        return content;
    }

    private static String addPostgresDependency(String content) {
        String postgreDependency = """
                \t\t<dependency>
                \t\t\t<groupId>org.springframework.boot</groupId>
                \t\t\t<artifactId>spring-boot-starter-data-jpa</artifactId>
                \t\t</dependency>
                \t\t\t<dependency>
                \t\t\t<groupId>org.postgresql</groupId>
                \t\t\t<artifactId>postgresql</artifactId>
                \t\t\t<scope>runtime</scope>
                \t\t</dependency>\n""";
        int dependenciesEndIndex = content.indexOf("</dependencies>");
        if (dependenciesEndIndex != -1) {
            content = content.substring(0, dependenciesEndIndex) + postgreDependency + content.substring(dependenciesEndIndex);
        }
        return content;
    }

    private static String addMongoDependency(String content) {
        String mongoDependency = """
                \t\t<dependency>
                \t\t\t<groupId>org.springframework.boot</groupId>
                \t\t\t<artifactId>spring-boot-starter-data-mongodb</artifactId>
                \t\t</dependency>\n""";
        int dependenciesEndIndex = content.indexOf("</dependencies>");
        if (dependenciesEndIndex != -1) {
            content = content.substring(0, dependenciesEndIndex) + mongoDependency + content.substring(dependenciesEndIndex);
        }
        return content;
    }

    private static void createDockerBuildProperties(String projectName,Path srcPath) throws IOException {
        Path pomFile = srcPath.resolve("docker_build.properties");
        String dockerProps = "service-name=" +projectName+"\n"
                +"service-type=springboot\n"
                +"main-module="+projectName+"-web\n"
                +"context-path="+projectName;
        Files.write(pomFile, dockerProps.getBytes());
    }
        private static void createWebProject(String projectName,Path basePath,
                                             List<String> addOns, String database) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-web").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        String content = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/MainApplication");
        String mainClassContent = content.replace("{projectName}",projectName.replace("-", "."));
        Path controllerPackagePath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", "."));
        Path mainClassPath = controllerPackagePath.resolve("MainApplication.java");
        Files.createDirectories(mainClassPath.getParent());
        Files.write(mainClassPath, mainClassContent.getBytes());
        createAndWriteFile("/src/main/resources/static/parent/SystemParameter/SystemParameterController", "{groupId}", "com.multispring." + projectName.replace("-", "."), controllerPackagePath.resolve("controller"), "SystemParameterController.java");

        // Todo : mentions changes here so made these
        //  createAndWriteFile("/src/main/resources/static/parent/SystemParameter/BaseRestResponse", "{groupId}", "com.multispring." + projectName.replace("-", "."), controllerPackagePath.resolve("controller"), "SystemParameterController.java");
        String staticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/BaseRestResponse");
        String baseContent = staticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path dynamicProjectFilePath = controllerPackagePath.resolve("response");
        Path baseRestResponsePath = dynamicProjectFilePath.resolve("BaseRestResponse.java");
        Files.createDirectories(baseRestResponsePath.getParent());
        Files.write(baseRestResponsePath, baseContent.getBytes());

        String baseRestListStaticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/RestListResponse");
        String baseRestListContent = baseRestListStaticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path baseRestListResponsePath = dynamicProjectFilePath.resolve("RestListResponse.java");
        Files.createDirectories(baseRestListResponsePath.getParent());
        Files.write(baseRestListResponsePath, baseRestListContent.getBytes());

        String pageMetaDataStaticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/PageMetaData");
        String pageMetaDataContent = pageMetaDataStaticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path pageMetaDataResponsePath = dynamicProjectFilePath.resolve("PageMetaData.java");
        Files.createDirectories(pageMetaDataResponsePath.getParent());
        Files.write(pageMetaDataResponsePath, pageMetaDataContent.getBytes());

        String baseRestStaticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/BaseRest");
        String baseRestContent = baseRestStaticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path baseRestPath = dynamicProjectFilePath.resolve("BaseRest.java");
        Files.createDirectories(baseRestPath.getParent());
        Files.write(baseRestPath, baseRestContent.getBytes());

        String baseResponseStaticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/BaseResponse");
        String baseResponseContent = baseResponseStaticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path baseResponsePath = dynamicProjectFilePath.resolve("BaseResponse.java");
        Files.createDirectories(baseResponsePath.getParent());
        Files.write(baseResponsePath, baseResponseContent.getBytes());

        String baseBuilderStaticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ "/src/main/resources/static/parent/SystemParameter/BaseBuilder");
        String baseBuilderContent = baseBuilderStaticFilePath.replace("{projectName}", projectName.replace("-", "."));
        Path baseBuilderResponsePath = dynamicProjectFilePath.resolve("BaseBuilder.java");
        Files.createDirectories(baseBuilderResponsePath.getParent());
        Files.write(baseBuilderResponsePath, baseBuilderContent.getBytes());

        Path resourcesPath = basePath.resolve(projectName+"-web").resolve("src").resolve("main").resolve("resources");
        Files.createDirectories(resourcesPath);
        Path propertiesFile = resourcesPath.resolve("application.properties");
        Files.createFile(propertiesFile);
        if(addOns != null && addOns.contains("kafka")){
            String kafkaProperty = "kafka.topic= Topics to be added #Add your topic name";
            String updated= kafkaProperty+"\n"+"server.servlet.context-path=/"+projectName+"\n"+ "#hit the swagger  :  http://localhost:8091/"+projectName+"/swagger-ui/index.html#/\n";
            Files.write(propertiesFile,updated.getBytes(),StandardOpenOption.APPEND);
        }
        if(database != null && database.equals("mongo")){
            String mongoDb = "spring.data.mongodb.uri= mongodb://localhost:27017/your_database_name #Replace your database name\n";
            Files.write(propertiesFile,mongoDb.getBytes(), StandardOpenOption.APPEND);
        }
        else if(database !=null && database.equals("postgres")){
            String postgresDb = """
                    spring.datasource.url= jdbc:postgresql://localhost:5432/your_database_name #Replace your database name
                    spring.datasource.username= #Add your database username
                    spring.datasource.password= #Add your database password
                    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL81Dialect
                    spring.jpa.hibernate.ddl-auto= update
                    server.port = 8091 \n""";
            Files.write(propertiesFile,postgresDb.getBytes(), StandardOpenOption.APPEND);
        }
            createAndWritePomFile(basePath.resolve(projectName + "-web"), "pom.xml", "/src/main/resources/static/parent/ControllerPom.xml", projectName);
        }



    private static void createModelProject(String projectName,Path basePath,String database,List<String> addOns) throws IOException {
            File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-model").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", "."));
        Files.createDirectories(mainClassPath);
        Path entityClassPath = mainClassPath.resolve("entity").resolve("SystemParameter.java");
        Files.createDirectories(entityClassPath.getParent());

        if(database != null && database.equals("mongo")) {
            String entityContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterMongoEntity");
            String updatedEntityContent = entityContent.replace("{groupId}",projectName.replace("-", "."));
            Files.write(entityClassPath,updatedEntityContent.getBytes());
            Path entityBaseMongoClassPath = mainClassPath.resolve("entity").resolve("BaseMongoEntity.java");
            String entityBaseMongoContent = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/SystemParameter/BaseMongoEntity");
            String updatedBaseMongoEntityContent = entityBaseMongoContent.replace("{projectName}", projectName.replace("-", "."));
            Files.write(entityBaseMongoClassPath, updatedBaseMongoEntityContent.getBytes());
        } else if (database != null && database.equals("postgres")) {
            String entityContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterPostgresEntity");
            String updatedEntityContent = entityContent.replace("{groupId}",projectName.replace("-", "."));
            Files.write(entityClassPath,updatedEntityContent.getBytes());
            Path entityBaseClassPath = mainClassPath.resolve("entity").resolve("BaseEntity.java");
            String entityBaseContent = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/SystemParameter/BaseEntity");
            String updatedBaseEntityContent = entityBaseContent.replace("{projectName}", projectName.replace("-", "."));
            Files.write(entityBaseClassPath, updatedBaseEntityContent.getBytes());
        }

        Path enumClassPath = mainClassPath.resolve("enums");
        Files.createDirectories(enumClassPath);

        Path ErrorCodePath = enumClassPath.resolve("ErrorCode.java") ;
        Files.createDirectories(ErrorCodePath.getParent());
        String errorCodePathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/ErrorCode");
        String updatedErrorCodePathContent = errorCodePathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(ErrorCodePath,updatedErrorCodePathContent.getBytes());

        Path constantClassPath = mainClassPath.resolve("constant").resolve("ApiPath.java");
        Files.createDirectories(constantClassPath.getParent());
        String apiPathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/ApiPath");
        String updatedApiPathContent = apiPathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(constantClassPath,updatedApiPathContent.getBytes());
        if(database != null && database.equalsIgnoreCase("mongo")) {
            Path collectionClassPath = mainClassPath.resolve("constant").resolve("CollectionName.java");
            String collection = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/contents/CollectionName");
            String updatedCollection = collection.replace("{projectName}", projectName.replace("-", "."));
            Files.write(collectionClassPath, updatedCollection.getBytes());
        } else if (database != null && database.equalsIgnoreCase("postgres")) {
            Path collectionClassPath = mainClassPath.resolve("constant").resolve("TableName.java");
            String collection = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/contents/TableName");
            String updatedCollection = collection.replace("{projectName}", projectName.replace("-", "."));
            Files.write(collectionClassPath, updatedCollection.getBytes());
        }
        Path exceptionClassPath = mainClassPath.resolve("exception");
        Files.createDirectories(exceptionClassPath);

        Path BaseBusinessException = exceptionClassPath.resolve("BaseBusinessException.java") ;
        Files.createDirectories(BaseBusinessException.getParent());
        String exceptionPathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/BaseBusinessException");
        String updatedExceptionPathContent = exceptionPathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(BaseBusinessException,updatedExceptionPathContent.getBytes());

        Path requestClassPath = mainClassPath.resolve("request");
        Files.createDirectories(requestClassPath);
        Path SystemParameterRequest = requestClassPath.resolve("SystemParameterRequest.java") ;
        Files.createDirectories(SystemParameterRequest.getParent());
        String requestPathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterRequest");
        String updatedRequestPathContent = requestPathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(SystemParameterRequest,updatedRequestPathContent.getBytes());
        Path responseClassPath = mainClassPath.resolve("response");
        Files.createDirectories(responseClassPath);
        Path SystemParameterResponse = responseClassPath.resolve("SystemParameterResponse.java") ;
        Files.createDirectories(SystemParameterResponse.getParent());
        Path BaseResponse = responseClassPath.resolve("BaseResponse.java") ;
        Files.createDirectories(BaseResponse.getParent());

        String baseResponsePathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/BaseResponse");
        String updatedBaseResponsePathContent = baseResponsePathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(BaseResponse,updatedBaseResponsePathContent.getBytes());

        String responsePathContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterResponse");
        String updatedResponsePathContent = responsePathContent.replace("{projectName}",projectName.replace("-", "."));
        Files.write(SystemParameterResponse,updatedResponsePathContent.getBytes());
        Path pomFile = basePath.resolve(projectName+"-model").resolve("pom.xml");
        if(addOns != null && addOns.contains("kafka")){
            Path kafkaMessageRequest = requestClassPath.resolve("kafka").resolve("MessageEmailRequest.java") ;
            Files.createDirectories(kafkaMessageRequest.getParent());
            String content = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/MessageEmailRequest");
            String updatedContent = content.replace("{projectName}",projectName.replace("-", "."));
            Files.write(kafkaMessageRequest,updatedContent.getBytes());
        }
        Files.createFile(pomFile);
        String pomContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/ModelPom.xml");
        String updatedContent = pomContent.replace("{artifactId}", projectName).replace("{groupId}","com.multispring."+projectName.replace("-", "."));
        Files.write(pomFile, updatedContent.getBytes());
    }



    private static void createServiceProject(String projectName,Path basePath,List<String> addOns) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-service").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", "."));
        Files.createDirectories(mainClassPath.getParent());
        String systemService = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterService");
        String serviceContent = systemService.replace("{groupId}","com.multispring."+projectName.replace("-", "."));
        Path controllerPath = mainClassPath.resolve("SystemParameterService.java");
        Files.createDirectories(controllerPath.getParent());
        Files.write(controllerPath, serviceContent.getBytes());
        if(addOns != null && addOns.contains("kafka")) {
            String content = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/KafkaPublisherService");
            String updatedContent = content.replace("{projectName}",projectName.replace("-", "."));
            Path kafka = mainClassPath.resolve("kafka").resolve("KafkaPublisherService.java");
            Files.createDirectories(kafka.getParent());
            Files.write(kafka, updatedContent.getBytes());
        }
        createAndWritePomFile(basePath.resolve(projectName + "-service"), "pom.xml", "/src/main/resources/static/parent/ServicePom.xml", projectName);
    }
    private static void createServiceImplProject(String projectName,Path basePath,
                                                 List<String> addOns, String database) throws IOException, InterruptedException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-service-impl").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", "."));
        Files.createDirectories(mainClassPath.getParent());

        String systemServiceImpl = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/SystemParameterServiceImpl");
        String updatedServiceImplContent = systemServiceImpl.replace("{groupId}","com.multispring."+projectName.replace("-", "."));
        if(database != null && database.contains("mongo")){
            Path servicePath = mainClassPath.resolve("helper").resolve("CopyProperties.java");
            Files.createDirectories(servicePath.getParent());
            String copyProperties = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/SystemParameter/CopyProperties");
            String updatedCopyPropertiesContent = copyProperties.replace("{groupId}","com.multispring."+projectName.replace("-", "."));
            Files.write(servicePath, updatedCopyPropertiesContent.getBytes());

            updatedServiceImplContent = updatedServiceImplContent.replace("import java.util.Date;", "import java.time.LocalDateTime;")
                      .replace("new Date()", "LocalDateTime.now()")
                      .replace("PropertyUtils.copyProperties(systemParameterResponse,systemParameter);","CopyProperties.copyResponse(systemParameterResponse,systemParameter);");

            String copyPropertiesImport = "import {groupId}.helper.CopyProperties;\n".replace("{groupId}","com.multispring."+projectName.replace("-", "."));
            String importTargetCode = "import java.util.Optional;";
            int importInsertionIndex = updatedServiceImplContent.indexOf(importTargetCode);
            if (importInsertionIndex != -1) {
                StringBuilder modifiedContent = new StringBuilder(updatedServiceImplContent);
                modifiedContent.insert(importInsertionIndex, copyPropertiesImport);
                updatedServiceImplContent = modifiedContent.toString();
            }
        }
        Path servicePath = mainClassPath.resolve("serviceimpl").resolve("SystemParameterServiceImpl.java");
        Files.createDirectories(servicePath.getParent());
        Files.write(servicePath, updatedServiceImplContent.getBytes());

        if(addOns != null && addOns.contains("kafka")) {
            String content = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/KafkaPublisherServiceImpl");
            Path kafka = mainClassPath.resolve("kafka").resolve("KafkaPublisherServiceImpl.java");
            Files.createDirectories(kafka.getParent());
            String updatedContent = content.replace("{projectName}",projectName.replace("-", "."));
            Files.write(kafka, updatedContent.getBytes());        }
        createAndWritePomFile(basePath.resolve(projectName + "-service-impl"), "pom.xml", "/src/main/resources/static/parent/ServiceImplPom.xml", projectName);
    }
    private static void createRepositoryProject(String projectName,Path basePath,String database) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-repository").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", ".")).resolve("repository").resolve("SystemParameterRepository.java");
        Files.createDirectories(mainClassPath.getParent());
        if(database != null && database.equals("mongo")) {
            String systemRepo = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/SystemParameter/SystemParameterMongoRepository");
            String repoContent = systemRepo.replace("{groupId}", "com.multispring." + projectName.replace("-", "."));
            Files.write(mainClassPath, repoContent.getBytes());
        } else if (database != null && database.equals("postgres")) {
            String systemRepo = PomFileReader.readPomFile(System.getProperty("user.dir") + "/src/main/resources/static/parent/SystemParameter/SystemParameterPostgresRepository");
            String repoContent = systemRepo.replace("{groupId}", "com.multispring." + projectName.replace("-", "."));
            Files.write(mainClassPath, repoContent.getBytes());
        }
        createAndWritePomFile(basePath.resolve(projectName + "-repository"), "pom.xml", "/src/main/resources/static/parent/RepositoryPom.xml", projectName);
    }
    private static void createConfigurationProject(String projectName,Path basePath) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-configuration").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", ".")).resolve("config").resolve("OpenApiConfigurations.java");
        Files.createDirectories(mainClassPath.getParent());
        String content = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/contents/OpenApiConfigurations");
        String configContent = content.replace("{projectName}",projectName.replace("-", "."));
        Files.createDirectories(mainClassPath.getParent());
        Files.write(mainClassPath, configContent.getBytes());
        Path pomFile = basePath.resolve(projectName+"-configuration").resolve("pom.xml");
        Files.createFile(pomFile);
        String pomContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/configPom.xml");
        String updatedContent = pomContent.replace("{artifactId}", projectName).replace("{groupId}","com.multispring."+projectName.replace("-", "."));
        Files.write(pomFile, updatedContent.getBytes());
    }

    private static void createCoverageProject(String projectName,Path basePath) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath = basePath.resolve(projectName+"-aggregate-coverage-report");
        Files.createDirectories(srcPath);
        createAndWritePomFile(srcPath, "pom.xml", "/src/main/resources/static/parent/CoveragePom.xml", projectName);
    }
    private static void createOutboundProject(String projectName,Path basePath) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path srcPath =basePath.resolve(projectName+"-outbound").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcPath);
        Path mainClassPath = srcPath.resolve("com").resolve("multispring").resolve(projectName.replace("-", "."));
        Files.createDirectories(mainClassPath.getParent());
        createAndWritePomFile(basePath.resolve(projectName + "-outbound"), "pom.xml", "/src/main/resources/static/parent/OutboundPom.xml", projectName);
    }

    private static void createNonProdProject(String projectName,Path basePath) throws IOException {
        File projectDir = new File(projectName);
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                throw new IOException("Failed to create the project directory.");
            }
        }
        Path deploymentPath =basePath.resolve("deployment");
        Files.createDirectories(deploymentPath);
        Path qa2ClassPath = deploymentPath.resolve("qa2");
        Files.createDirectories(qa2ClassPath.getParent());

        createAndWriteFile("/src/main/resources/static/parent/nonprod/qa2/values", "{groupId}",  projectName.toUpperCase().replace("-","_"),  deploymentPath.resolve("qa2"), "values.yaml");
        Path qa2Path =basePath.resolve("properties").resolve("qa2");
        Files.createDirectories(qa2Path);
        Path applicationPropertiesPath =qa2Path.resolve("application.properties");
        Path logbackPath =qa2Path.resolve("logback.xml");

        Path resourcesPath = basePath.resolve(projectName+"-web").resolve("src").resolve("main").resolve("resources").resolve("application.properties");
        String propertiesContent = PomFileReader.readPomFile(resourcesPath.toString());
        Files.write(applicationPropertiesPath,propertiesContent.getBytes());

        String logbackContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/nonprod/logback");
        String updatedLogbackContent = logbackContent.replace("{projectName}",projectName.replace("-","."));
        Files.write(logbackPath,updatedLogbackContent.getBytes());

        Path jenkinsPath =basePath.resolve("Jenkinsfile");
        String jenkinsContent = PomFileReader.readPomFile(System.getProperty("user.dir")+"/src/main/resources/static/parent/nonprod/jenkins");
        String modifiedContent = jenkinsContent.replace("{groupId}",projectName);
        Files.write(jenkinsPath,modifiedContent.getBytes());

    }

    public static void zipSpringBootProject(String projectDirectory, OutputStream out) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(out);
        File projectDir = new File(projectDirectory);
        zipDirectory(projectDir, projectDir.getName(), zipOut);
        zipOut.close();
    }

    private static void zipDirectory(File directoryToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (directoryToZip.isHidden()) {
            return;
        }
        if (directoryToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }

            File[] children = directoryToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectory(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }

        FileInputStream fis = new FileInputStream(directoryToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }

        fis.close();
    }
    private static void createAndWriteFile(String staticPath, String stringToReplace, String projectName, Path path, String fileName) throws IOException {
        String staticFilePath = PomFileReader.readPomFile(System.getProperty("user.dir")+ staticPath);
        String content = staticFilePath.replace(stringToReplace, projectName);
        Path dynamicProjectFilePath = path.resolve(fileName);
        Files.createDirectories(dynamicProjectFilePath.getParent());
        Files.write(dynamicProjectFilePath, content.getBytes());
    }


    private static void createAndWritePomFile(Path basePath, String fileName, String staticFileName, String projectName) throws IOException {
        Path pomFile = basePath.resolve(fileName);
        Files.createFile(pomFile);
        String pomContent = PomFileReader.readPomFile(System.getProperty("user.dir")+ staticFileName);
        String updatedContent = pomContent.replace("{artifactId}", projectName).replace("{groupId}","com.multispring."+ projectName.replace("-", "."));
        Files.write(pomFile, updatedContent.getBytes());
    }
}




