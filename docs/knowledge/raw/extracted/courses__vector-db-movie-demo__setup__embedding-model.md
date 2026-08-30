# Choose an embedding model

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 1 – Embedding modelOn this page
# Choose an embedding model

Before configuring your backend or running any scripts, decide which embedding model you want to use.

The embedding model will convert each movie plot summary into a fixed-size vector. The dimension of that vector depends on your chosen model, and you must configure this number in your backend.

## Clone the repo​

If you haven’t done so already, start by cloning the project repository:

```bash
git clone https://bitbucket.org/chromawallet/vector-db-movie-demo.gitcd vector-db-movie-demo
```

## Create your .env file​

Navigate to the python/ folder and create your .env file by copying the template:

```bash
cd pythoncp .env.template .env
```

You will edit this file in the next step.

## Select your model​

Open the .env file in your editor and pick one of the available EMBEDDING_MODEL options by uncommenting the relevant line.

For example:

```env
EMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
```

This field controls which sentence embedding model will be used when vectorizing movie summaries.

noteMake sure to note the dimensions of the model you choose, as you'll need it in the next lesson to set up your Rell module.

Feel free to substitute any other model from the Hugging Face Hub, provided it's compatible with the sentence-transformers library and produces dense vectors.

## Model overview​

| 
| Model| Dimensions| Notes
| all-MiniLM-L6-v2| 384| Small and fast
| all-mpnet-base-v2| 768| Balanced, a good default choice
| all-mpnet-large-v2| 1024| High quality but slower
| intfloat/e5-large-v2| 1024| Instruction-tuned, best for search
| BAAI/bge-large-en-v1.5| 1024| Alternative model with excellent performance

## Choosing the right model​

Larger models can give better semantic results but require more memory and processing time.

If you're using a GPU, you can likely use any of the models listed above without issues.

If you're running on CPU, you may want to stick to a smaller model to avoid slow performance.

Next, you’ll use the model’s dimensions to configure your Rell module in chromia.yml.
