# 2026-10-31T14:15:29.634161300
import vitis

client = vitis.create_client()
client.set_workspace(path="IncredibleAI")

comp = client.create_hls_component(name = "vitis",cfg_file = ["hls_config.cfg"],template = "empty_hls_component")

cfg = client.get_config_file(path="E:\workspace\IntelliJ\IncredibleAI\vitis\hls_config.cfg")

cfg.set_values(key="syn.file", values=["./src/bitarith.hpp", "./src/kernel.cpp", "./src/kernel.hpp", "./src/layers.cpp", "./src/layers.hpp", "./src/params.hpp", "./src/predictions.hpp", "./src/types.hpp", "./src/window_buffer.hpp"])

cfg.set_values(key="tb.file", values=["./src/all_tests.cpp", "./src/bitarith_test.hpp", "./src/image.hpp", "./src/output.hpp", "./src/params.hpp", "./src/predictions_test.hpp", "./src/tb.cpp", "./src/window_buffer_test.hpp", "./src/xunit.hpp"])

vitis.dispose()

