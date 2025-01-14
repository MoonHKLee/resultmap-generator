package moonhyuk.lee.resultmap.generator;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.Set;

/**
 * MyBatis Mapper XML 생성 전담 클래스
 */
class MapperXmlGenerator {

    private final ProcessingEnvironment processingEnv;
    private final Messager messager;
    private final ResultMapGenerator resultMapGenerator;

    public MapperXmlGenerator(ProcessingEnvironment processingEnv, Messager messager) {
        this.processingEnv = processingEnv;
        this.messager = messager;
        this.resultMapGenerator = new ResultMapGenerator(processingEnv, messager);
    }

    /**
     * 전체 Mapper XML을 구성하는 메서드.
     */
    public String generateMapperXml(String namespace,
                                    Set<? extends Element> tableElements,
                                    Set<? extends Element> embeddableElements) {

        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper
                        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                """);
        sb.append("<mapper namespace=\"")
                .append(namespace)
                .append("\">\n\n");

        // 1) @Embeddable 클래스들에 대한 <resultMap> 생성
        for (Element embeddableClass : embeddableElements) {
            sb.append(resultMapGenerator.generateResultMap(embeddableClass, true));
        }

        // 2) @Table 클래스들에 대한 <resultMap> 생성
        for (Element tableClass : tableElements) {
            sb.append(resultMapGenerator.generateResultMap(tableClass, false));
        }

        sb.append("</mapper>\n");
        return sb.toString();
    }
}
