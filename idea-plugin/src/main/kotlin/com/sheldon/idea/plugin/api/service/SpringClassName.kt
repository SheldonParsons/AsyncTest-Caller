package com.sheldon.idea.plugin.api.service

object SpringClassName {

    enum class RequestMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE;

        companion object {
            fun from(name: String?): RequestMethod? {
                return entries.find { it.name == name }
            }
        }
    }

    val SPRING_REQUEST_RESPONSE: Array<String> = arrayOf(
        "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse"
    )

    var SPRING_CONTROLLER_ANNOTATION: Set<String> = mutableSetOf(
        SPRING_WEB_CONTROLLER, SPRING_WEB_REST_CONTROLLER
    )

    var SPRING_WEB_CONTROLLER_ANNOTATION: Set<String> = mutableSetOf(
        SPRING_WEB_CONTROLLER
    )

    const val SPRING_WEB_CONTROLLER = "org.springframework.stereotype.Controller"
    const val SPRING_WEB_REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"

    // --- Java ---
    const val JAVA_PREFIX = "java."
    const val JAVA_BASE_OBJECT = "java.lang.Object"
    const val JAVAX_ANN_RESOURCE = "javax.annotation.Resource"
    const val JAKARTA_ANN_RESOURCE = "jakarta.annotation.Resource"
    const val JAVA_MATH_BIG_DECIMAL = "java.math.BigDecimal"
    const val JAVA_TIME_TEMPORAL = "java.time.temporal.Temporal"


    // --- Spring ---
    const val SPRING_ANN_AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"
    const val SPRING_ANN_VALUE = "org.springframework.beans.factory.annotation.Value"



    //@RequestMapping
    const val REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping"

    // @RequestMapping注解属性名称常量
    const val ATTR_VALUE = "value"
    const val ATTR_PATH = "path"
    const val ATTR_METHOD = "method"
    const val ATTR_PARAMS = "params"
    const val ATTR_HEADERS = "headers"
    const val ATTR_CONSUMES = "consumes"
    const val ATTR_PRODUCES = "produces"

    // 函数属性注解
    const val REQUEST_BODY_ANNOTATION = "org.springframework.web.bind.annotation.RequestBody"
    const val REQUEST_PARAM_ANNOTATION = "org.springframework.web.bind.annotation.RequestParam"
    const val MODEL_ATTRIBUTE_ANNOTATION = "org.springframework.web.bind.annotation.ModelAttribute"
    const val REQUEST_PART_ANNOTATION = "org.springframework.web.bind.annotation.RequestPart"
    const val PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable"
    const val COOKIE_VALUE_ANNOTATION = "org.springframework.web.bind.annotation.CookieValue"
    const val REQUEST_HEADER_ANNOTATION = "org.springframework.web.bind.annotation.RequestHeader"

    //file
    const val MULTI_PART_FILE = "org.springframework.web.multipart.MultipartFile"
    const val JAVAX_PART = "javax.servlet.http.Part"
    const val JAKARTA_PART = "jakarta.servlet.http.Part"

    // --- Headers相关 ---
    const val CONTENT_TYPE = "content-type"
    const val APPLICATION_JSON = "application/json"
    const val CONTAINER_HTTP_HEADERS = "org.springframework.http.HttpHeaders"
    const val CONTAINER_JAVA_UTIL_MAP = "java.util.Map"
    const val CONTAINER_MULTI_VALUE_MAP = "org.springframework.util.MultiValueMap"

    // --- 属性名 ---
    const val ATTR_NAME = "name" // value 的别名
    const val ATTR_REQUIRED = "required"
    const val ATTR_DEFAULT_VALUE = "defaultValue"
    const val VAL_DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n" // Spring 内部的 "无默认值" 常量

    // --- 参数类型 ---
    const val HTTP_ENTITY = "org.springframework.http.HttpEntity"
    const val REQUEST_ENTITY = "org.springframework.http.RequestEntity"


    // @RequestMapping注解语法糖注解
    const val GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping"
    const val POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping"
    const val PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping"
    const val DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping"
    const val PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping"

    val SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS: Set<String> = setOf(
        REQUEST_MAPPING_ANNOTATION, GET_MAPPING, DELETE_MAPPING, PATCH_MAPPING, POST_MAPPING, PUT_MAPPING
    )

    val METHOD_ANNOTATION_MAP = mapOf(
        GET_MAPPING to RequestMethod.GET,
        POST_MAPPING to RequestMethod.POST,
        PUT_MAPPING to RequestMethod.PUT,
        DELETE_MAPPING to RequestMethod.DELETE,
        PATCH_MAPPING to RequestMethod.PATCH
    )

    // --- 空值校验 ---
    val REQUIRED_ANNOTATIONS = setOf(
        "javax.validation.constraints.NotNull",
        "javax.validation.constraints.NotEmpty",
        "javax.validation.constraints.NotBlank",
        "jakarta.validation.constraints.NotNull",
        "jakarta.validation.constraints.NotEmpty",
        "jakarta.validation.constraints.NotBlank"
    )


    val SPRING_REQUEST_MAPPING_ANNOTATIONS: Set<String> = setOf(
        REQUEST_MAPPING_ANNOTATION, GET_MAPPING, DELETE_MAPPING, PATCH_MAPPING, POST_MAPPING, PUT_MAPPING
    )

    const val REQUEST_HEADER_DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n"

    const val ESCAPE_REQUEST_HEADER_DEFAULT_NONE = "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002\\n\\t\\t\\t\\t\\n"


    //Spring Boot Actuator Annotations
    const val ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Endpoint"
    const val WEB_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint"
    const val CONTROLLER_ENDPOINT_ANNOTATION =
        "org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint"
    const val REST_CONTROLLER_ENDPOINT_ANNOTATION =
        "org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint"

    const val READ_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation"
    const val WRITE_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.WriteOperation"
    const val DELETE_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.DeleteOperation"
    const val SELECTOR_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Selector"

    val ENDPOINT_ANNOTATIONS: Set<String> = setOf(
        ENDPOINT_ANNOTATION,
        WEB_ENDPOINT_ANNOTATION,
        CONTROLLER_ENDPOINT_ANNOTATION,
        REST_CONTROLLER_ENDPOINT_ANNOTATION
    )
    val ENDPOINT_OPERATION_ANNOTATIONS: Set<String> = setOf(
        READ_OPERATION_ANNOTATION, WRITE_OPERATION_ANNOTATION, DELETE_OPERATION_ANNOTATION
    )

    val IGNORED_PARAM_TYPES = setOf(
        // Servlet
        "javax.servlet.ServletRequest",
        "javax.servlet.http.HttpServletRequest",
        "javax.servlet.http.HttpServletResponse",
        "javax.servlet.http.HttpSession",
        // Spring Boot 3 Servlet
        "jakarta.servlet.ServletRequest",
        "jakarta.servlet.http.HttpServletRequest",
        "jakarta.servlet.http.HttpServletResponse",
        "jakarta.servlet.http.HttpSession",
        // Spring Context / UI
        "org.springframework.ui.Model",
        "org.springframework.validation.BindingResult",
        "org.springframework.validation.Errors",
        "org.springframework.web.bind.support.SessionStatus",
        "org.springframework.web.context.request.WebRequest",
        "org.springframework.web.context.request.NativeWebRequest",

        // IO (Stream 处理通常不作为普通 API 参数展示)
        "java.io.InputStream",
        "java.io.OutputStream",
        "java.io.Reader",
        "java.io.Writer",
    )
}

data class RequestPathInfo(
    val path: String, // 完整路径 /api/user/create
    val method: String // GET, POST, etc.
)