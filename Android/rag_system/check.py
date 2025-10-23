import faiss, numpy as np, torch

x = torch.rand(2331, 384)
x_np = np.ascontiguousarray(x.detach().cpu().numpy(), dtype=np.float32)
faiss.normalize_L2(x_np)
print("Normalize OK, no crash ✅")

