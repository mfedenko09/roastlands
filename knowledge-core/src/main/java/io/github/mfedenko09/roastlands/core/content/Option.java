package io.github.mfedenko09.roastlands.core.content;

public sealed interface Option permits TextOption, OutputOption, CompileErrorOption, ExceptionOption {
}
