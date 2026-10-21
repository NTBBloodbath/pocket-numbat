use std::any::Any;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::sync::{Arc, Mutex, OnceLock};

use codespan_reporting::term::termcolor::Buffer;
use codespan_reporting::term::{self, Config};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use numbat::diagnostic::{ErrorDiagnostic, ResolverDiagnostic};
use numbat::module_importer::BuiltinModuleImporter;
use numbat::resolver::CodeSource;
use numbat::{Context, FormatOptions, InterpreterSettings, NumbatError};

static EXCHANGE_RATES_INITIALIZED: OnceLock<()> = OnceLock::new();

pub struct Engine {
    ctx: Context,
}

impl Engine {
    pub fn new() -> Result<Self, String> {
        let mut ctx = Context::new(BuiltinModuleImporter::default());
        ctx.set_terminal_width(Some(40));
        ctx.load_currency_module_on_demand(true);

        let mut settings = InterpreterSettings {
            print_fn: Box::new(|_| {}),
        };
        let _ = ctx
            .interpret_with_settings(&mut settings, "use prelude", CodeSource::Internal)
            .map_err(|err| format_numbat_error(&ctx, &err))?;

        Ok(Self { ctx })
    }

    pub fn eval(&mut self, input: &str) -> Result<String, String> {
        let printed = Arc::new(Mutex::new(String::new()));
        let printed_handle = Arc::clone(&printed);

        let mut settings = InterpreterSettings {
            print_fn: Box::new(move |markup| {
                if let Ok(mut buffer) = printed_handle.lock() {
                    buffer.push_str(&markup.to_string());
                }
            }),
        };

        let interpreted = self
            .ctx
            .interpret_with_settings(&mut settings, input, CodeSource::Text);
        drop(settings);

        let (statements, result) =
            interpreted.map_err(|err| format_numbat_error(&self.ctx, &err))?;

        let result_markup = result.to_markup(
            statements.last(),
            self.ctx.dimension_registry(),
            true,
            false,
            &FormatOptions::default(),
        );

        let mut output = printed.lock().map(|s| s.clone()).unwrap_or_default();
        output.push_str(&result_markup.to_string());

        Ok(output.trim().to_string())
    }

    pub fn set_exchange_rates(xml: &str) {
        if EXCHANGE_RATES_INITIALIZED.set(()).is_ok() {
            Context::set_exchange_rates(xml);
        }
    }
}

fn format_numbat_error(ctx: &Context, err: &NumbatError) -> String {
    let diagnostics = match err {
        NumbatError::ResolverError(err) => err.diagnostics(),
        NumbatError::NameResolutionError(err) => err.diagnostics(),
        NumbatError::TypeCheckError(err) => err.diagnostics(),
        NumbatError::RuntimeError(err) => ResolverDiagnostic {
            resolver: ctx.resolver(),
            error: err,
        }
        .diagnostics(),
    };

    if diagnostics.is_empty() {
        return err.to_string();
    }

    let mut buffer = Buffer::no_color();
    let config = Config::default();
    for diagnostic in &diagnostics {
        let _ = term::emit(&mut buffer, &config, &ctx.resolver().files, diagnostic);
    }

    String::from_utf8_lossy(&buffer.into_inner()).into_owned()
}

fn panic_message(payload: Box<dyn Any + Send>) -> String {
    if let Some(message) = payload.downcast_ref::<&str>() {
        (*message).to_owned()
    } else if let Some(message) = payload.downcast_ref::<String>() {
        message.clone()
    } else {
        "panic in native code".to_owned()
    }
}

fn throw(env: &mut JNIEnv, message: impl AsRef<str>) {
    let _ = env.throw_new("java/lang/RuntimeException", message.as_ref());
}

/// # Safety
///
/// `env` must be a valid JNI environment for the calling thread.
#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_io_github_ntbbloodbath_pocketnumbat_calc_NumbatNative_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    match catch_unwind(AssertUnwindSafe(Engine::new)) {
        Ok(Ok(engine)) => Box::into_raw(Box::new(engine)) as jlong,
        Ok(Err(message)) => {
            throw(&mut env, message);
            0
        }
        Err(payload) => {
            throw(&mut env, panic_message(payload));
            0
        }
    }
}

/// # Safety
///
/// `handle` must be null or a pointer returned by `nativeCreate` that has not
/// already been destroyed.
#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_io_github_ntbbloodbath_pocketnumbat_calc_NumbatNative_nativeDestroy(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if handle != 0 {
            unsafe {
                drop(Box::from_raw(handle as *mut Engine));
            }
        }
    }));

    if let Err(payload) = result {
        throw(&mut env, panic_message(payload));
    }
}

/// # Safety
///
/// `handle` must be null or a pointer returned by `nativeCreate` that has not
/// already been destroyed. `input` must be a valid Java string reference.
#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_io_github_ntbbloodbath_pocketnumbat_calc_NumbatNative_nativeInterpret(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    input: JString,
) -> jstring {
    if handle == 0 {
        throw(&mut env, "native engine handle is null");
        return std::ptr::null_mut();
    }

    let result = catch_unwind(AssertUnwindSafe(|| {
        let input: String = env
            .get_string(&input)
            .map_err(|err| err.to_string())?
            .into();

        let engine = unsafe { &mut *(handle as *mut Engine) };
        engine.eval(&input)
    }));

    match result {
        Ok(Ok(output)) => match env.new_string(output) {
            Ok(output) => output.into_raw(),
            Err(err) => {
                throw(&mut env, err.to_string());
                std::ptr::null_mut()
            }
        },
        Ok(Err(message)) => {
            throw(&mut env, message);
            std::ptr::null_mut()
        }
        Err(payload) => {
            throw(&mut env, panic_message(payload));
            std::ptr::null_mut()
        }
    }
}

/// # Safety
///
/// `xml` must be a valid Java string reference.
#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_io_github_ntbbloodbath_pocketnumbat_calc_NumbatNative_nativeSetExchangeRates(
    mut env: JNIEnv,
    _class: JClass,
    xml: JString,
) {
    let result = catch_unwind(AssertUnwindSafe(|| -> Result<(), String> {
        let xml: String = env.get_string(&xml).map_err(|err| err.to_string())?.into();

        Engine::set_exchange_rates(&xml);
        Ok(())
    }));

    match result {
        Ok(Ok(())) => {}
        Ok(Err(message)) => throw(&mut env, message),
        Err(payload) => throw(&mut env, panic_message(payload)),
    }
}

#[cfg(test)]
mod tests {
    use super::Engine;

    fn engine() -> Engine {
        Engine::new().expect("engine should start")
    }

    #[test]
    fn evaluates_arithmetic_with_units() {
        let mut engine = engine();
        let output = engine
            .eval("2 m + 30 cm")
            .expect("expression should evaluate");
        assert!(output.contains("230 cm"), "unexpected output: {output}");
    }

    #[test]
    fn keeps_state_between_calls() {
        let mut engine = engine();
        engine
            .eval("let x = 2")
            .expect("definition should evaluate");
        let output = engine.eval("x * 3").expect("expression should evaluate");
        assert!(output.contains('6'), "unexpected output: {output}");
    }

    #[test]
    fn reports_type_errors_and_recovers() {
        let mut engine = engine();
        let error = engine
            .eval("1 m + 1 s")
            .expect_err("expression should fail");
        assert!(
            error.to_lowercase().contains("dimension"),
            "unexpected error: {error}"
        );

        let output = engine.eval("2 + 2").expect("engine should recover");
        assert!(output.contains('4'), "unexpected output: {output}");
    }

    #[test]
    fn captures_print_output() {
        let mut engine = engine();
        let output = engine
            .eval("print(\"hello\")")
            .expect("print should evaluate");
        assert!(output.contains("hello"), "unexpected output: {output}");
    }

    #[test]
    fn prelude_is_available() {
        let mut engine = engine();
        let output = engine.eval("sin(0)").expect("prelude function should work");
        assert!(output.contains('0'), "unexpected output: {output}");
    }
}
