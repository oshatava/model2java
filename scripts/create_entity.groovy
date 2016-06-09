package ru.arcsinus.spotster.api.scripts

import groovy.json.JsonSlurper

class Base {
    static final String LIST_BASE = "java.util.List"
    static final String LIST_IMPL = "java.util.ArrayList"
    static final String CALL_TYPE_CALL = "Call"
    static final String CALL_TYPE_RX = "Observable"

    public static final String START_CUSTOMER_CODE = '/*---------------------------------------CUSTOMER CODE START--------------------------------------*/'
    public static final String END_CUSTOMER_CODE = '/*---------------------------------------CUSTOMER CODE END----------------------------------------*/'


    String callType = CALL_TYPE_RX

    String content
    def path
    def outputPath
    String template
    File outputFile
    String packageName
    String plural
    Boolean shouldInitializeLists = true;

    String customerCode = ""

    protected String getCustomerCodeFromFile(String filename){
        StringBuilder ret = new StringBuilder();
        boolean inCode = false;
        File f = new File(filename)
        if(f.exists()) {
            boolean isFirst = true;
            f.eachLine { _line ->
                String line = _line.trim();
                if (line.compareToIgnoreCase(END_CUSTOMER_CODE) == 0)
                    inCode = false;

                if (inCode) {
                    if(!isFirst)
                        ret.append("\r\n");
                    ret.append(_line);
                    isFirst = false;
                }

                if (line.compareToIgnoreCase(START_CUSTOMER_CODE) == 0)
                    inCode = true;
            }
        }

        return ret.toString();
    }

    protected void saveCustomerCode(String filename){
        customerCode = getCustomerCodeFromFile(filename);
    }

    protected String applyCustomerCode(String inData){
        return inData.replace('{CUSTOMER_CODE}', customerCode);
    }

    Base(filename, output, packageName, template){
        this.path = filename
        this.template = template
        this.outputPath = output
        this.packageName = packageName
    }

    public String jsonTypeToJavaType(String jsonType){
        switch (jsonType.toLowerCase()){
            case "any": return "String"
            case "number": return "int"
            case "double": return "double" // add this manually
            case "string": return "String"
            case "array": return "${LIST_BASE}<String>"
            case "date": return "String" //todo think about it
            case "null": return "String"
            case "object": return "String"
            case "boolean": return "Boolean"
            default: return jsonType;
        }
    }

    String createPropertyDeclaration(String name, String type) {
        StringBuilder ret = new StringBuilder()
        String pType = jsonTypeToJavaType(type);
        //field
        ret.append("    public ${pType} ${name};\r\n")

        return ret.toString()
    }

    public void setPlural(def model ){
        if(model.plural==null)
            plural = "${model.name}s"
        else
            plural = "${model.plural}"
    }

    public String createNameConsistentWithJavaNameConvention(v){
        String name = v.toString();
        String betterIdea = Character.toUpperCase(name.charAt(0)).toString() + name.substring(1);
        return betterIdea;
    }

    public void setCommon(name,_extends, _implements){
        content = template;
        content = content
                .replace("{ENTITY_NAME}", createNameConsistentWithJavaNameConvention(name))
                .replace("{PACKAGE_NAME}", packageName)
                .replace("{PACKAGE_NAME}", packageName)
                .replace("{USER_NAME}", System.getProperty("user.name"))
                .replace("{DATE}", new Date().toLocaleString())
        if(!_extends.isEmpty())
            content = content.replace("{EXTENDS}", "extends "+_extends)
        else
            content = content.replace("{EXTENDS}", "")

        if(!_implements.isEmpty())
            content = content.replace("{IMPLEMENTS}", "implements "+_implements)
        else
            content = content.replace("{IMPLEMENTS}", "")

    }

    void parse(){}

}


class Model extends Base {


	Model(filename, output, packageName, template){
        super(filename, output, packageName, template)
        parse()
	}

    String createRelation(String name, o) {

        StringBuilder ret = new StringBuilder()
        String rType = o.type
        String pType = o.model

        if(rType==null || pType==null)
            return ""

        Boolean isList = rType.compareToIgnoreCase("hasMany")==0
        Boolean isOne = rType.compareToIgnoreCase("hasOne")==0
        Boolean isBelongsTo = rType.compareToIgnoreCase("belongsTo")==0

        // we not support other relations yet
        if(! (isList||isOne||isBelongsTo)) return "";

        if(isList)
            pType = "${LIST_BASE}<${pType}>"
        else if(isOne||isBelongsTo)
            pType = ""+pType
        String gsName = createNameConsistentWithJavaNameConvention(name)
        //field
        ret.append("    @Relation(Relation.RelationType.${rType})\r\n")
        ret.append("    private ${pType} ${name};\r\n")

        if(isList) {
            String listInitialisation = " if(${name}==null){ ${name} = new ${LIST_IMPL}<>();}; "
            //getter
            ret.append("    public ${pType} get${gsName}(){ ${listInitialisation} return ${name}; }\r\n")
            //setter
            ret.append("    public void set${gsName}(${pType} value){ get${gsName}().clear(); get${gsName}().addAll(value); }\r\n")
        }else if(isOne) {
            String listInitialisation = " if(${name}==null){ ${name} = new ${pType}();}; "
            //getter
            ret.append("    public ${pType} get${gsName}(){ ${listInitialisation} return ${name}; }\r\n")
            //setter
            ret.append("    public void set${gsName}(${pType} value){  ${name} = value; }\r\n")
        }else if(isBelongsTo) {
            String listInitialisation = " if(${name}==null){ ${name} = new ${pType}();}; "
            //getter
            ret.append("    public ${pType} get${gsName}(){ ${listInitialisation} return ${name}; }\r\n")
            //setter
            ret.append("    public void set${gsName}(${pType} value){  ${name} = value; }\r\n")
        }


        return ret.toString()
    }

    String createProperty(String name, o) {
        StringBuilder ret = new StringBuilder()
        String pType = jsonTypeToJavaType(o.type);

        String gsName = createNameConsistentWithJavaNameConvention(name)

        //field
        ret.append("    private ${pType} ${name};\r\n")
        //getter
        ret.append("    public ${pType} get${gsName}(){ return ${name}; }\r\n")
        //setter
        ret.append("    public void set${gsName}(${pType} value){ ${name} = value; }\r\n")

        return ret.toString()
    }

    void parse(){

		println "-------------------------------------"
		println this.path
        println this.outputPath

		def inputFile = new File(this.path)
		def model = new JsonSlurper().parseText(inputFile.text)
        setPlural(model)
        String outputFileName = this.outputPath+"\\"+createNameConsistentWithJavaNameConvention(model.name)+".java"

		outputFile = new File(outputFileName)

        saveCustomerCode(outputFileName)

        setCommon(model.name, ""/*model.base*/, "")

        StringBuilder annotations = new StringBuilder()

        annotations.append("\r\n@Model(\"${model.name}\")")
        annotations.append("\r\n@Plural(\"${plural}\")")

        StringBuilder properties = new StringBuilder()
        properties.append("\r\n")


        // Create properties
        if(model.idInjection){
            properties.append(createProperty("id", [ "type":"int" ] ));
            properties.append("\r\n")
        }

        if(model.properties!=null) {
            model.properties.each { k, v ->
                properties.append(createProperty(k, v));
                properties.append("\r\n")
            }
        }

        // Create relations
        StringBuilder relations = new StringBuilder()
        if(model.relations!=null) {
            model.relations.each { k, v ->
                relations.append(createRelation(k, v));
                relations.append("\r\n")
            }
        }

        // Update body of class
        String body = properties.toString() + relations.toString()

        if(!body.isEmpty())
            content = content.replace("{BODY}", body)
        else
            content = content.replace("{BODY}", "")

        // todo add imports if needed
        content = content.replace("{IMPORTS}", "import com.oshatava.data.services.annotations.*;")

        content = content.replace("{ANNOTATIONS}", annotations.toString())

        content = applyCustomerCode(content);
        content = content.replace("{CALL_TYPE}", callType)

        outputFile.write(content, "UTF-8")

	}

}

class Service extends Base {

    String modelsPackageName

    Service(filename, output, packageName, template, modelsPackageName){
        super(filename, output, packageName, template)
        this.modelsPackageName = modelsPackageName
        parse()
    }

    public static String preparePath(String path){
        path.replaceAll(/:([a-zA-Z0-9_]+)/){ fullMatch, paramName -> '{'+paramName+'}' }
    }

    public static performAnnotationForParam(String path, String arg){
        if(!path.toLowerCase().contains('{'+arg.toLowerCase()+'}'))
            return ""
        "@Path(\"${arg}\")"
    }

    String createArgList(String path, def accepts){

        StringBuilder args = new StringBuilder();

        if(accepts instanceof  List) {
            Boolean isFirst = true;
            accepts.each {
                if(it.type!=null && it.arg!=null) {
                    String type = jsonTypeToJavaType(it.type.toString())
                    String name = it.arg.toString()
                    String annotation = performAnnotationForParam(path, name)
                    if (type != null) {
                        if (!isFirst)
                            args.append(", ")

                        args.append("${annotation} ${type} ${name}")
                        isFirst = false
                    }
                }
            }
        }else{
            if(accepts.type!=null && accepts.arg!=null) {
                String type = jsonTypeToJavaType(accepts.type.toString())
                String name = accepts.arg.toString()
                String annotation = performAnnotationForParam(path, name)
                if (type != null) {
                    args.append("${annotation} ${type} ${name}")
                }
            }
        }

        args.toString()
    }

    String createMethod(String name, o) {
        StringBuilder ret = new StringBuilder()
        String description = o.description
        def accepts = o.accepts
        def returns = o.returns
        def http = o.http
        if(http==null)
            return ""
        if(http.verb == null || http.path == null)
            return ""

        // Response class
        String gsName = createNameConsistentWithJavaNameConvention(name)
        String verb = "${http.verb}".toLowerCase()
        String methodName = "${verb}${gsName}"
        String returnClassName = "${gsName}Response";
        String returnClassDefinition = createResponseClassBasedOnReturnsDefinition(returnClassName, returns);
        if(returnClassDefinition!=null)
            ret.append("\r\n    ${returnClassDefinition}")

        String http_path
        // Request method
        if(description!=null)
            ret.append("\r\n    // ${description}")
        if(http!=null && http.path!=null && http.verb!=null) {
            http_path = "${plural}"+preparePath(http.path.toString())
            String http_verb = http.verb.toString().toUpperCase();
            ret.append("\r\n    @${http_verb}(\"${http_path}\")")
        }
        String argss = createArgList(http_path, accepts)

        ret.append("\r\n    {CALL_TYPE}<${returnClassName}> ${methodName}(${argss});")

        return ret.toString()
    }

    String createResponseClassBasedOnReturnsDefinition(String s, def o) {
        StringBuilder ret = new StringBuilder();

        ret.append("\r\n    class ${s}{")
        if(o!=null){
            if( o instanceof List){
                o.each{ v ->
                    if(v.arg!=null && v.type!=null) {
                        String property = createPropertyDeclaration(v.arg.toString(), v.type.toString())
                        ret.append("\r\n        ${property}")
                    }
                }
            }else if(o.arg!=null && o.type!=null){
                String property = createPropertyDeclaration(o.arg.toString(), o.type.toString())
                ret.append("\r\n        ${property}")
            }
        }

        ret.append("\r\n    }")

        ret.toString()
    }

    void parse(){

        println "-------------------------------------"
        println this.path
        println this.outputPath

        def inputFile = new File(this.path)
        def model = new JsonSlurper().parseText(inputFile.text)
        setPlural(model)
        String outputFileName = this.outputPath+"\\"+createNameConsistentWithJavaNameConvention(model.name)+"Service.java"

        outputFile = new File(outputFileName)
        saveCustomerCode(outputFileName)

        setCommon(model.name, ""/*model.base*/, "")

        // Create methods
        StringBuilder methods = new StringBuilder()
        if(model.methods!=null) {
            model.methods.each { k, v ->
                methods.append(createMethod(k, v));
                methods.append("\r\n")
            }
        }

        StringBuilder relations = new StringBuilder();
        if(model.relations!=null) {
            boolean isFirst = true;
            model.relations.each { k, v ->
                if (!isFirst)
                    relations.append(",")
                relations.append("\\\"${k}\\\"");
                isFirst = false;
            }
        }
        // Update body of class
        String body = methods.toString()

        if(!body.isEmpty())
            content = content.replace("{BODY}", body)
        else
            content = content.replace("{BODY}", "")


        content = content.replace("{INCLUDE_ALL_RELATIONS}", relations.toString());

        // todo add imports if needed
        content = content.replace("{IMPORTS}", "import com.oshatava.data.services.annotations.*;\r\nimport ${this.modelsPackageName}.*;")

        content = content.replace("{ANNOTATIONS}", "")
        content = content.replace("{PLURAL}", "${plural}")
        content = applyCustomerCode(content)
        content = content.replace("{CALL_TYPE}", callType)

        outputFile.write(content, "UTF-8")

    }

}


class App {

    def models = []
    def services = []

	App(modelsPath, javasPath, servicesPath,
        packageName, servicePackageName,
        tplFileName, serviceTplFileName){

		println "models path:${modelsPath}"
		println "package path:${javasPath}"

        String tpl = "";
        if(tplFileName==null){
            tpl = new File("entity_template.tpl").text
        }else{
            tpl = new File(tplFileName).text
        }


        String serviceTpl = "";
        if(tplFileName==null){
            serviceTpl = new File("service_template.tpl").text
        }else{
            serviceTpl = new File(serviceTplFileName).text
        }


        new File(modelsPath).eachFileMatch(~/.*.json/) { file ->
			models << new Model (file.canonicalPath,  new File(javasPath).canonicalFile.toString(), packageName, tpl )
            services << new Service(file.canonicalPath,  new File(servicesPath).canonicalFile.toString(), servicePackageName, serviceTpl, packageName )
		}

	}
}


new App("../models", "../entities", "../services",
        "ru.arcsinus.spotster.api.entities", "ru.arcsinus.spotster.api.services",
        "entity_template.tpl", "service_template.tpl");

/*
println Service.preparePath("/var/:id/order/:orderId")
*/