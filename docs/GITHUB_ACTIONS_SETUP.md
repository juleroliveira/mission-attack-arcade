# Configuração das Ações de CI/CD (kolabs-dev/mobile-actions)

Os workflows do **`kolabs-dev/mobile-actions`** foram instalados no repositório:
- [`.github/workflows/android-upload.yml`](../.github/workflows/android-upload.yml): Constrói o `.aab`, assina e faz o upload automático para a Google Play Store.
- [`.github/workflows/android-promote.yml`](../.github/workflows/android-promote.yml): Promove uma versão existente entre faixas (ex: de `internal` para `production`).

---

## 🔑 Segredos do Repositório (Repository Secrets)

Para que o GitHub Actions consiga assinar o app e se comunicar com a Google Play Store, acesse:
👉 **[Configurações de Secrets do Repositório](https://github.com/juleroliveira/mission-attack-arcade/settings/secrets/actions)**

E adicione os seguintes segredos (clicando em **"New repository secret"**):

| Nome do Segredo (Secret Name) | Valor a Colocar |
| :--- | :--- |
| **`ANDROID_KEYSTORE_BASE64`** | Conteúdo do arquivo `playstore/keystore_base64.txt` (gerado localmente na pasta `scratch/playstore/keystore_base64.txt`) |
| **`ANDROID_KEYSTORE_PASSWORD`** | `arcade1984release` |
| **`ANDROID_KEY_ALIAS`** | `missionattack` |
| **`ANDROID_KEY_PASSWORD`** | `arcade1984release` |
| **`GOOGLE_SERVICE_ACCOUNT_JSON_BASE64`** | Conteúdo do JSON da conta de serviço do Google Cloud em Base64 *(veja abaixo como gerar)* |

---

## 🛠️ Como Gerar o `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64`

1. Acesse o [Google Play Console](https://play.google.com/console).
2. Vá em **Configurações do desenvolvedor** ➔ **Acesso à API**.
3. Clique em **Vincular projeto do Google Cloud** (ou selecione um existente).
4. Em *Contas de serviço*, clique em **Criar conta de serviço**.
5. No Google Cloud Console que abrir, crie a chave no formato **JSON** e baixe para o seu computador.
6. De volta ao Play Console, conceda a essa conta de serviço a permissão de **Gerente de lançamentos** (Release Manager).
7. Para converter o JSON baixado para Base64 no PowerShell, execute:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("caminho\para\seu-service-account.json"))
   ```
8. Copie a string gerada e cole no secret `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64`.

---

## 🚀 Como Executar o Lançamento Automático

- **Via Tag Git:** Ao criar e subir uma tag de versão (ex: `git tag v1.0.1` e `git push origin v1.0.1`), a ação executa automaticamente.
- **Via Painel do GitHub:** Acesse a aba **Actions** ➔ selecione **Android Upload & Release** ➔ clique em **Run workflow**, escolha a faixa (`internal`, `alpha`, `beta` ou `production`) e clique no botão verde!
