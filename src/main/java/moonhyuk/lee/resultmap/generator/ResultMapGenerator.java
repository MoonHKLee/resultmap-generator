package moonhyuk.lee.resultmap.generator;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * 개별 클래스(엔티티 or 임베더블)에 대한 <resultMap> 생성 로직
 */
class ResultMapGenerator {

    private final ProcessingEnvironment processingEnv;
    private final Messager messager;

    public ResultMapGenerator(ProcessingEnvironment processingEnv, Messager messager) {
        this.processingEnv = processingEnv;
        this.messager = messager;
    }

    /**
     * 하나의 클래스에 대한 <resultMap>을 생성
     * @param classElement 대상 클래스 Element
     * @param isEmbeddable @Embeddable 여부
     */
    public String generateResultMap(Element classElement, boolean isEmbeddable) {
        // 클래스 정보
        String className = ((TypeElement) classElement).getQualifiedName().toString();
        String simpleName = classElement.getSimpleName().toString();

        // @Table("...") 값 얻기 (Embeddable인 경우 null 가능)
        Table tableAnn = classElement.getAnnotation(Table.class);
        String tableName = (tableAnn != null) ? tableAnn.value() : simpleName;

        // resultMap 아이디
        String resultMapId = simpleName + "ResultMap";

        StringBuilder sb = new StringBuilder();
        sb.append("    <!-- ")
                .append(tableName)
                .append(isEmbeddable ? " (@Embeddable)" : "")
                .append(" -->\n")
                .append("    <resultMap id=\"")
                .append(resultMapId)
                .append("\" type=\"")
                .append(className)
                .append("\">\n");

        // 클래스 내 필드를 순회하며 <id>, <result>, <association>, <collection> 등을 구성
        for (Element field : classElement.getEnclosedElements()) {
            if (field.getKind() != ElementKind.FIELD) {
                continue;
            }
            appendFieldMapping(sb, field);
        }

        sb.append("    </resultMap>\n\n");
        return sb.toString();
    }

    /**
     * 필드 하나에 대한 매핑(<id>, <result>, <association>, <collection>) 처리
     */
    private void appendFieldMapping(StringBuilder sb, Element field) {
        String fieldName = field.getSimpleName().toString();

        // 스프링 데이터 관련 애노테이션
        Id idAnn = field.getAnnotation(Id.class);
        Column colAnn = field.getAnnotation(Column.class);
        MappedCollection mappedCollAnn = field.getAnnotation(MappedCollection.class);

        // (A) @Id + @Column
        if (idAnn != null && colAnn != null) {
            sb.append("        <id property=\"")
                    .append(fieldName)
                    .append("\" column=\"")
                    .append(colAnn.value())
                    .append("\"/>\n");
        }
        // (B) 일반 @Column
        else if (colAnn != null) {
            sb.append("        <result property=\"")
                    .append(fieldName)
                    .append("\" column=\"")
                    .append(colAnn.value())
                    .append("\"/>\n");
        }
        // (C) @MappedCollection → <collection>
        else if (mappedCollAnn != null) {
            handleMappedCollection(sb, field, fieldName);
        }
        // (D) 필드 타입이 @Embeddable 이면 → <association resultMap="..." />
        else {
            handleEmbeddableType(sb, field, fieldName);
        }
    }

    /**
     * @MappedCollection 처리: <collection resultMap="..."/>
     */
    private void handleMappedCollection(StringBuilder sb, Element field, String fieldName) {
        if (!(field.asType() instanceof DeclaredType)) {
            return;
        }

        DeclaredType declaredType = (DeclaredType) field.asType();
        if (declaredType.getTypeArguments().isEmpty()) {
            return;
        }

        // 첫 번째 제네릭 타입의 FQCN
        String genericTypeName = declaredType.getTypeArguments().get(0).toString();
        String nestedSimpleName = genericTypeName.substring(genericTypeName.lastIndexOf('.') + 1);
        String nestedResultMapId = nestedSimpleName + "ResultMap";

        sb.append("        <collection property=\"")
                .append(fieldName)
                .append("\" resultMap=\"")
                .append(nestedResultMapId)
                .append("\"/>\n");
    }

    /**
     * 필드가 @Embeddable 인지 확인하여 <association resultMap="..."/> 처리
     */
    private void handleEmbeddableType(StringBuilder sb, Element field, String fieldName) {
        Element fieldTypeElement = processingEnv.getTypeUtils().asElement(field.asType());
        if (fieldTypeElement == null) {
            return;
        }

        if (fieldTypeElement.getAnnotation(Embeddable.class) != null) {
            String nestedSimpleName = fieldTypeElement.getSimpleName().toString();
            String nestedResultMapId = nestedSimpleName + "ResultMap";

            sb.append("        <association property=\"")
                    .append(fieldName)
                    .append("\" resultMap=\"")
                    .append(nestedResultMapId)
                    .append("\"/>\n");
        }
    }
}