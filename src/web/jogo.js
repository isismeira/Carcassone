/* ---------------------------------------------------------------------------
   Carcassonne — tela de partida
   Toda regra vive no modelo Java; esta camada só desenha o estado e envia as
   ações do jogador para /api.
--------------------------------------------------------------------------- */

(() => {
  "use strict";

  const PECA = 96;          // deve acompanhar --peca no CSS
  const PECA_ESTREITA = 76;
  const ZOOM_MIN = 0.35;
  const ZOOM_MAX = 2.2;

  const MODOS = {
    ai: "contra IAs",
    human: "contra jogadores",
    mixed: "IAs e jogadores"
  };

  const PAPEIS = {
    LADRAO: "Ladrão",
    CAVALEIRO: "Cavaleiro",
    MONGE: "Monge",
    FAZENDEIRO: "Fazendeiro"
  };

  const MARCADORES = {
    LADRAO: "L",
    CAVALEIRO: "C",
    MONGE: "M",
    FAZENDEIRO: "F"
  };

  const TERRENOS = {
    CIDADE: "Cidade",
    ESTRADA: "Estrada",
    CAMPO: "Campo",
    MOSTEIRO: "Mosteiro"
  };

  const LADOS = {
    NORTE: "Norte",
    LESTE: "Leste",
    SUL: "Sul",
    OESTE: "Oeste"
  };

  const CORES = {
    VERMELHO: "#b02a2a",
    AZUL: "#1e3a6e",
    VERDE: "#2f7d42",
    AMARELO: "#c9a227",
    PRETO: "#2b2b2b",
    CINZA: "#6b7280"
  };

  const el = {
    mundo: document.querySelector("[data-mundo]"),
    tabuleiro: document.querySelector("[data-tabuleiro]"),
    nome: document.querySelector("[data-nome]"),
    modo: document.querySelector("[data-modo]"),
    cor: document.querySelector("[data-cor]"),
    restantes: document.querySelector("[data-restantes]"),
    colocadas: document.querySelector("[data-colocadas]"),
    pontos: document.querySelector("[data-pontos]"),
    meeples: document.querySelector("[data-meeples]"),
    descartadas: document.querySelector("[data-descartadas]"),
    blocoDescarte: document.querySelector("[data-bloco-descarte]"),
    pecaImagem: document.querySelector("[data-peca-imagem]"),
    pecaBrasao: document.querySelector("[data-peca-brasao]"),
    pecaId: document.querySelector("[data-peca-id]"),
    aviso: document.querySelector("[data-aviso]"),
    dica: document.querySelector("[data-dica]"),
    painelColocacao: document.querySelector("[data-painel-colocacao]"),
    alocacao: document.querySelector("[data-alocacao]"),
    alocacaoDica: document.querySelector("[data-alocacao-dica]"),
    alocacaoAviso: document.querySelector("[data-alocacao-aviso]"),
    segmentos: document.querySelector("[data-segmentos]"),
    fim: document.querySelector("[data-fim]"),
    fimTexto: document.querySelector("[data-fim-texto]")
  };

  const camera = { x: 0, y: 0, escala: 1 };
  let estado = null;

  const ladoDaPeca = () =>
    window.matchMedia("(max-width: 900px)").matches ? PECA_ESTREITA : PECA;

  /* --- Conversa com o servidor -------------------------------------------- */

  async function chamar(rota, corpo) {
    const resposta = await fetch(rota, {
      method: corpo ? "POST" : "GET",
      headers: corpo ? { "Content-Type": "application/json" } : undefined,
      body: corpo ? JSON.stringify(corpo) : undefined
    });
    if (!resposta.ok) {
      const detalhe = await resposta.json().catch(() => ({}));
      throw new Error(detalhe.erro || `falha em ${rota}`);
    }
    return resposta.json();
  }

  async function carregar() {
    try {
      estado = await chamar("/api/estado");
      desenhar();
      centralizar();
    } catch (erro) {
      // Sem partida no servidor: volta para a tela inicial.
      encerrar("Nenhuma partida em andamento. Comece pela tela inicial.");
    }
  }

  async function agir(rota, corpo) {
    try {
      estado = await chamar(rota, corpo);
      desenhar();
    } catch (erro) {
      el.aviso.textContent = erro.message;
    }
  }

  /* --- Desenho ------------------------------------------------------------ */

  function desenhar() {
    if (!estado) return;

    el.nome.textContent = estado.jogador.nome;
    el.cor.style.background = CORES[estado.jogador.cor] || CORES.VERMELHO;
    el.modo.textContent = MODOS[estado.modo] ? `· ${MODOS[estado.modo]}` : "";

    el.restantes.textContent = estado.baralho.restantes;
    el.colocadas.textContent = estado.tabuleiro.length;
    el.pontos.textContent = estado.jogador.pontos;
    el.meeples.textContent = estado.jogador.meeples;
    el.descartadas.textContent = estado.baralho.descartadas;
    el.blocoDescarte.hidden = estado.baralho.descartadas === 0;

    el.painelColocacao.hidden = estado.aguardandoMeeple;
    el.aviso.textContent = estado.aguardandoMeeple ? "" : (estado.mensagem || "");
    el.alocacaoAviso.textContent = estado.aguardandoMeeple ? (estado.mensagem || "") : "";

    desenharPecaAtual();
    desenharOpcoesMeeple();
    desenharTabuleiro();

    if (estado.encerrada) {
      encerrar(textoDeEncerramento());
    }
  }

  function desenharPecaAtual() {
    const peca = estado.pecaAtual;
    document.querySelectorAll("[data-girar]").forEach((botao) => {
      botao.disabled = estado.aguardandoMeeple || !peca;
    });
    if (!peca) {
      el.pecaImagem.removeAttribute("src");
      el.pecaImagem.alt = "";
      el.pecaBrasao.hidden = true;
      el.pecaId.textContent = "—";
      preencherBordas(null);
      return;
    }
    el.pecaImagem.src = `assets/tiles/${peca.imagem}`;
    el.pecaImagem.alt = `Peça ${peca.id}`;
    el.pecaImagem.style.transform = `rotate(${peca.rotacao * 90}deg)`;
    el.pecaBrasao.hidden = !peca.brasao;
    el.pecaId.textContent = peca.id.replace("#", " nº ");
    preencherBordas(peca.bordas);
  }

  function desenharOpcoesMeeple() {
    el.alocacao.hidden = !estado.aguardandoMeeple;
    el.segmentos.replaceChildren();
    if (!estado.aguardandoMeeple) return;

    for (const segmento of estado.segmentos) {
      const botao = document.createElement("button");
      botao.type = "button";
      botao.className = "alocacao__opcao";
      botao.disabled = segmento.ocupado || estado.jogador.meeples === 0;
      const papel = PAPEIS[segmento.papel] || segmento.papel;
      const terreno = TERRENOS[segmento.terreno] || segmento.terreno;
      const local = segmento.lados.length
        ? segmento.lados.map((lado) => LADOS[lado] || lado).join(" e ")
        : "centro da peça";
      botao.textContent = `${papel} · ${terreno} · ${local}`;
      botao.addEventListener("click", () =>
        agir("/api/meeple", { segmento: segmento.indice }));
      el.segmentos.append(botao);
    }
    el.alocacaoDica.textContent = estado.jogador.meeples > 0
      ? "Escolha a região indicada pelos lados da peça ou continue sem alocar."
      : "Você não tem meeples disponíveis. Continue para a próxima peça.";
  }

  function preencherBordas(bordas) {
    for (const lado of ["NORTE", "LESTE", "SUL", "OESTE"]) {
      const alvo = document.querySelector(`[data-borda="${lado}"]`);
      if (!alvo) continue;
      const valor = bordas ? bordas[lado] : null;
      alvo.textContent = valor ? (TERRENOS[valor] || valor) : "—";
    }
  }

  function desenharTabuleiro() {
    const lado = ladoDaPeca();
    el.mundo.replaceChildren();

    for (const cartela of estado.tabuleiro) {
      const div = document.createElement("div");
      div.className = "cartela"
        + (cartela.inicial ? " cartela--inicial" : "")
        + (cartela.meeples.length ? " cartela--com-meeple" : "");
      div.style.left = `${cartela.x * lado}px`;
      div.style.top = `${cartela.y * lado}px`;
      div.style.backgroundImage = `url("assets/tiles/${cartela.imagem}")`;
      div.style.transform = `rotate(${cartela.rotacao * 90}deg)`;
      if (cartela.brasao) {
        const brasao = document.createElement("img");
        brasao.className = "cartela__brasao";
        brasao.src = "assets/tiles/emblem.png";
        brasao.alt = "";
        brasao.style.setProperty("--contra-giro", `${-cartela.rotacao * 90}deg`);
        div.append(brasao);
      }
      cartela.meeples.forEach((meeple, indice) => {
        const marcador = document.createElement("span");
        marcador.className = "cartela__meeple";
        marcador.textContent = MARCADORES[meeple.papel] || "?";
        marcador.title = `${PAPEIS[meeple.papel] || meeple.papel} de ${meeple.nome}`;
        marcador.style.backgroundColor = CORES[meeple.cor] || CORES.VERMELHO;
        marcador.style.left = `${meeple.x * 100}%`;
        marcador.style.top = `${meeple.y * 100}%`;
        marcador.style.setProperty("--contra-giro", `${-cartela.rotacao * 90}deg`);
        div.append(marcador);
      });
      el.mundo.append(div);
    }

    for (const vaga of estado.candidatas) {
      const botao = document.createElement("button");
      botao.type = "button";
      botao.className = "vaga";
      botao.style.left = `${vaga.x * lado}px`;
      botao.style.top = `${vaga.y * lado}px`;
      botao.setAttribute("aria-label", `Encaixar em ${vaga.x}, ${vaga.y}`);
      botao.addEventListener("click", () => agir("/api/colocar", { x: vaga.x, y: vaga.y }));
      el.mundo.append(botao);
    }

    if (estado.aguardandoMeeple) {
      el.dica.textContent = estado.jogador.meeples > 0
        ? "A peça foi colocada. Escolha um segmento livre para alocar um meeple ou continue sem alocar."
        : "A peça foi colocada. Você não tem meeples disponíveis; continue para a próxima jogada.";
    } else if (estado.pecaAtual) {
      el.dica.textContent = estado.candidatas.length
        ? `${estado.candidatas.length} ${estado.candidatas.length === 1 ? "espaço aceita" : "espaços aceitam"} a peça nesta rotação. Gire para ver outras opções.`
        : "Nenhum espaço aceita a peça nesta rotação — gire-a.";
    } else {
      el.dica.textContent = "A partida terminou.";
    }

    aplicarCamera();
  }

  function encerrar(texto) {
    el.fimTexto.textContent = texto;
    el.fim.hidden = false;
  }

  function textoDeEncerramento() {
    const colocadas = estado.tabuleiro.length;
    const descartadas = estado.baralho.descartadas;
    let texto = `O baralho acabou. Você terminou com ${estado.jogador.pontos} pontos e encaixou ${colocadas} peças no tabuleiro`;
    texto += descartadas ? `, e ${descartadas} foram descartadas por não caber em lugar nenhum.` : ".";
    return texto;
  }

  /* --- Câmera: arrastar e ampliar ----------------------------------------- */

  function aplicarCamera() {
    el.mundo.style.transform =
      `translate(${camera.x}px, ${camera.y}px) scale(${camera.escala})`;
  }

  function centralizar() {
    if (!estado || !estado.tabuleiro.length) return;
    const lado = ladoDaPeca();
    const xs = estado.tabuleiro.map(c => c.x);
    const ys = estado.tabuleiro.map(c => c.y);
    const meioX = (Math.min(...xs) + Math.max(...xs) + 1) / 2;
    const meioY = (Math.min(...ys) + Math.max(...ys) + 1) / 2;
    camera.x = -meioX * lado * camera.escala;
    camera.y = -meioY * lado * camera.escala;
    aplicarCamera();
  }

  function ampliar(fator, focoX, focoY) {
    const anterior = camera.escala;
    const nova = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, anterior * fator));
    if (nova === anterior) return;
    // mantém sob o cursor o ponto do mundo que já estava ali
    const razao = nova / anterior;
    camera.x = focoX - (focoX - camera.x) * razao;
    camera.y = focoY - (focoY - camera.y) * razao;
    camera.escala = nova;
    aplicarCamera();
  }

  let arrastando = false;
  let ultimoX = 0;
  let ultimoY = 0;

  el.tabuleiro.addEventListener("pointerdown", (evento) => {
    if (evento.target.closest(".vaga")) return;
    arrastando = true;
    ultimoX = evento.clientX;
    ultimoY = evento.clientY;
    el.tabuleiro.classList.add("arrastando");
    el.tabuleiro.setPointerCapture(evento.pointerId);
  });

  el.tabuleiro.addEventListener("pointermove", (evento) => {
    if (!arrastando) return;
    camera.x += evento.clientX - ultimoX;
    camera.y += evento.clientY - ultimoY;
    ultimoX = evento.clientX;
    ultimoY = evento.clientY;
    aplicarCamera();
  });

  const soltar = (evento) => {
    if (!arrastando) return;
    arrastando = false;
    el.tabuleiro.classList.remove("arrastando");
    if (evento.pointerId !== undefined && el.tabuleiro.hasPointerCapture?.(evento.pointerId)) {
      el.tabuleiro.releasePointerCapture(evento.pointerId);
    }
  };

  el.tabuleiro.addEventListener("pointerup", soltar);
  el.tabuleiro.addEventListener("pointercancel", soltar);

  el.tabuleiro.addEventListener("wheel", (evento) => {
    evento.preventDefault();
    const area = el.tabuleiro.getBoundingClientRect();
    const focoX = evento.clientX - area.left - area.width / 2;
    const focoY = evento.clientY - area.top - area.height / 2;
    ampliar(evento.deltaY < 0 ? 1.12 : 1 / 1.12, focoX, focoY);
  }, { passive: false });

  /* --- Controles ----------------------------------------------------------- */

  document.querySelectorAll("[data-girar]").forEach((botao) => {
    botao.addEventListener("click", () =>
      agir("/api/rotacionar", { sentido: botao.dataset.girar }));
  });

  document.querySelector("[data-pular-meeple]").addEventListener("click", () =>
    agir("/api/meeple", { segmento: -1 }));

  document.querySelectorAll("[data-zoom]").forEach((botao) => {
    botao.addEventListener("click", () => {
      const passo = Number(botao.dataset.zoom);
      if (passo === 0) {
        camera.escala = 1;
        centralizar();
      } else {
        ampliar(passo > 0 ? 1.2 : 1 / 1.2, 0, 0);
      }
    });
  });

  document.addEventListener("keydown", (evento) => {
    if (evento.target.matches("input, textarea")) return;
    if (evento.key === "e" || evento.key === "E" || evento.key === "ArrowRight") {
      agir("/api/rotacionar", { sentido: "direita" });
    } else if (evento.key === "q" || evento.key === "Q" || evento.key === "ArrowLeft") {
      agir("/api/rotacionar", { sentido: "esquerda" });
    }
  });

  window.addEventListener("resize", () => {
    if (estado) desenharTabuleiro();
  });

  carregar();
})();
