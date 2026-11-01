#!/usr/bin/env bash
set -euo pipefail

# Build from FFmpeg's official, verified Git source; never substitute a desktop binary.
msc_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
msc_ffmpeg_source="${MSC_FFMPEG_SOURCE:?Set MSC_FFMPEG_SOURCE to the official FFmpeg n8.0.1 checkout}"
msc_expected_ref=894da5ca7d742e4429ffb2af534fcda0103ef593
test "$(git -C "$msc_ffmpeg_source" rev-parse HEAD)" = "$msc_expected_ref"
test -z "$(git -C "$msc_ffmpeg_source" status --porcelain)"
msc_ndk="${ANDROID_NDK_HOME:-${ANDROID_HOME:?Set ANDROID_HOME}/ndk/27.2.12479018}"
msc_toolchain="$msc_ndk/toolchains/llvm/prebuilt/linux-x86_64/bin"
msc_build_root="${MSC_NATIVE_BUILD_DIR:-/workspace/setup/msc-native-build}"
msc_jobs="${MSC_NATIVE_JOBS:-4}"
mkdir -p "$msc_build_root"

for msc_abi in ${MSC_NATIVE_ABIS:-x86_64 arm64-v8a armeabi-v7a}; do
  case "$msc_abi" in
    x86_64) msc_arch=x86_64; msc_cc=x86_64-linux-android26-clang ;;
    arm64-v8a) msc_arch=aarch64; msc_cc=aarch64-linux-android26-clang ;;
    armeabi-v7a) msc_arch=arm; msc_cc=armv7a-linux-androideabi26-clang ;;
    *) echo "Unsupported ABI: $msc_abi" >&2; exit 1 ;;
  esac
  mkdir -p "$msc_build_root/$msc_abi"
  cd "$msc_build_root/$msc_abi"
  "$msc_ffmpeg_source/configure" \
    --target-os=android --arch="$msc_arch" --enable-cross-compile \
    --cc="$msc_toolchain/$msc_cc" --cxx="$msc_toolchain/${msc_cc/clang/clang++}" \
    --ar="$msc_toolchain/llvm-ar" --ranlib="$msc_toolchain/llvm-ranlib" --strip="$msc_toolchain/llvm-strip" \
    --enable-pic --enable-static --disable-shared --disable-autodetect --disable-asm \
    --disable-gpl --disable-nonfree --disable-doc --disable-debug --disable-everything \
    --enable-small --enable-ffmpeg --disable-ffprobe --disable-ffplay \
    --enable-protocol=file,pipe,concat,data \
    --enable-demuxer=mov,flac,mp3,aac,ogg,matroska,wav \
    --enable-muxer=flac,mp4,ogg,adts,wav \
    --enable-decoder=flac,aac,alac,mp3,mp3float,opus,vorbis,pcm_s16le,pcm_s24le,pcm_s32le,pcm_f32le \
    --enable-encoder=flac,pcm_s16le \
    --enable-parser=aac,flac,mpegaudio,opus,vorbis \
    --enable-filter=aresample,anull,aformat --enable-bsf=aac_adtstoasc,extract_extradata \
    --extra-cflags='-O2 -fPIE -ffunction-sections -fdata-sections' \
    --extra-ldflags='-pie -Wl,--gc-sections -Wl,-z,max-page-size=16384' \
    > configure.log 2>&1
  make -j"$msc_jobs" ffmpeg > build.log 2>&1
  msc_destination="$msc_repo_root/embedded-clash/src/main/jniLibs/$msc_abi"
  mkdir -p "$msc_destination"
  cp ffmpeg "$msc_destination/libmsc_ffmpeg.so"
  "$msc_toolchain/llvm-strip" --strip-unneeded "$msc_destination/libmsc_ffmpeg.so"
  "$msc_toolchain/llvm-readelf" -h -l "$msc_destination/libmsc_ffmpeg.so" > elf-report.txt
  sha256sum "$msc_destination/libmsc_ffmpeg.so"
done

# Include this corresponding source archive with any binary distribution.
git -C "$msc_ffmpeg_source" archive --format=tar "$msc_expected_ref" | xz -T"$msc_jobs" > "$msc_build_root/ffmpeg-n8.0.1-source.tar.xz"
