package cn.edu.scnu.momentkeep.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * <p>此前所有异常都返回 HTTP 200、只用 {@code body.code} 区分，导致网关、云监控、
 * Nginx 都无法按状态码识别失败请求（连 404、参数错误都表现为 200），
 * 排查线上问题时极易误判。</p>
 *
 * <p>现在按语义返回真实 HTTP 状态码，而响应体结构 {@link Result} 保持不变，
 * 前端 {@code request.js} 的非 200 分支仍能取到 message 展示给用户。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 统一构造响应：状态码写入 HTTP 状态行，同时冗余写进 body.code */
    private static ResponseEntity<Result<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }

    /**
     * 兜底异常：真正的服务端故障。
     *
     * <p>记录完整堆栈，但只向前端返回脱敏后的通用文案。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常：{} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "系统异常，请联系管理员");
    }

    /**
     * 接口不存在 → 404。
     *
     * <p>这条曾经被兜底成 {@code 200 + "系统异常"}，导致「前端调用了后端没编译进去的新接口」
     * 这类问题极难定位（表现为前端只有一句含糊的系统异常）。</p>
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(Exception e, HttpServletRequest request) {
        log.warn("接口不存在：{} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "请求的接口不存在");
    }

    /** 请求方法不支持 → 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMethod());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法：" + e.getMethod());
    }

    /** Content-Type 不支持 → 415 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type：{}", e.getContentType());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的请求格式");
    }

    /** 业务异常 → 400（业务规则不满足属于客户端错误） */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** AI 配额超限 → 429（前端只提示文案，不清除登录态） */
    @ExceptionHandler(AiQuotaExceededException.class)
    public ResponseEntity<Result<Void>> handleQuotaExceeded(AiQuotaExceededException e) {
        log.warn("AI 配额超限：{}", e.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    /** 请求体参数校验失败（@Valid） → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = joinFieldErrors(e.getBindingResult().getFieldErrors());
        log.warn("参数校验失败：{}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** 表单/查询参数绑定校验失败 → 400 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = joinFieldErrors(e.getBindingResult().getFieldErrors());
        log.warn("参数绑定失败：{}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** 请求体不是合法 JSON 或字段类型不匹配 → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "请求参数格式错误");
    }

    /** 路径变量/查询参数类型不匹配 → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{} = {}", e.getName(), e.getValue());
        return build(HttpStatus.BAD_REQUEST, "参数 " + e.getName() + " 格式不正确");
    }

    /** 日期格式错误（如 2026-13-45） → 400 */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Result<Void>> handleDateTimeParse(DateTimeParseException e) {
        log.warn("日期解析失败：{}", e.getParsedString());
        return build(HttpStatus.BAD_REQUEST, "日期格式不正确，请使用 yyyy-MM-dd");
    }

    /** 缺少必填请求参数 → 400 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数：{}", e.getParameterName());
        return build(HttpStatus.BAD_REQUEST, "缺少必填参数：" + e.getParameterName());
    }

    /** 上传文件超过大小限制 → 413 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超限：{}", e.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "文件过大，请上传 5MB 以内的图片");
    }

    /** Spring Security 授权失败 → 403，与 SecurityConfig 中的处理器保持一致 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("拒绝访问：{}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, "没有权限访问该资源");
    }

    /** 拼接字段校验错误信息 */
    private static String joinFieldErrors(List<FieldError> errors) {
        String message = errors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return message.isEmpty() ? "参数校验失败" : message;
    }
}
