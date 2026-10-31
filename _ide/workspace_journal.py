# 2026-10-31T14:19:47.895807400
import vitis

client = vitis.create_client()
client.set_workspace(path="IncredibleAI")

cfg = client.get_config_file(path="E:\workspace\IntelliJ\IncredibleAI\vitis\hls_config.cfg")

cfg.set_values(key="syn.file", values=["./src/bitarith.hpp", "./src/kernel.cpp", "./src/kernel.hpp", "./src/layers.cpp", "./src/layers.hpp", "./src/predictions.hpp", "./src/types.hpp", "./src/window_buffer.hpp"])

cfg.set_values(key="syn.blackbox.file", values=[])

comp = client.get_component(name="vitis")
comp.run(operation="C_SIMULATION")

comp.run(operation="SYNTHESIS")

cfg = client.get_config_file(path="/e:/workspace/IntelliJ/IncredibleAI/vitis/hls_config.cfg")

cfg.set_values(key="relative_roots", values=[])

cfg.set_value(section="hls", key="syn.top", value="kifu")

comp.run(operation="C_SIMULATION")

comp.run(operation="C_SIMULATION")

comp.run(operation="SYNTHESIS")

comp.run(operation="PACKAGE")

comp.run(operation="PACKAGE")

