import domain.Produto;
import domain.Link;
import infra.HibernateUtil;
import service.ProdutoService;
import service.ServiceInterface;
import service.CrawlerService;

void main() {
    ProdutoService service = new ProdutoService();

    try {
        boolean menuAtivo = true;
        while (menuAtivo) {
            int opcaoSelecionada = menu();
            switch (opcaoSelecionada) {
                case 1:
                    adicionarProduto(service);
                    break;
                case 2:
                    listarProdutos(service);
                    break;
                case 3:
                    editarProdutos(service);
                    break;
                case 4:
                    deletarProdutos(service);
                    break;
                case 5:
                    executarCrawler(service);
                    break;
                case 6:
                    visualizarHistoricoCompleto(service);
                    break;
                case 0:
                    menuAtivo = false;
                    break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        }
    } finally {
        HibernateUtil.shutdown();
    }
}

public void adicionarProduto(ProdutoService service) {
    String sku = IO.readln("Digite a SKU do produto: ");
    String nome = IO.readln("Digite o nome do produto: ");
    String marca = IO.readln("Digite a marca do produto: ");
    String descricao = IO.readln("Digite a descricao do produto: ");
    Float preco = Float.parseFloat(IO.readln("Digite o preco do produto: "));

    Produto produto = new Produto(sku, nome, marca, descricao, preco);

    System.out.println("\nCadastro de links para monitoramento de preços:");
    System.out.println("O ideal é que cada produto tenha links de pelo menos 2 lojas diferentes (ex: Amazon, Kabum).");
    
    int linkCount = 0;
    boolean cadastrando = true;
    while (cadastrando || linkCount < 2) {
        if (linkCount < 2) {
            System.out.printf("\n[Mínimo de 2 links exigido] Cadastro do Link #%d:\n", linkCount + 1);
        } else {
            String resposta = IO.readln("\nDeseja cadastrar mais um link de loja? (S/N): ");
            if (resposta.equalsIgnoreCase("N")) {
                cadastrando = false;
                break;
            }
        }
        
        String loja = IO.readln("Nome da loja (ex: Amazon): ");
        String url = IO.readln("URL do produto na loja: ");
        
        if (loja.trim().isEmpty() || url.trim().isEmpty()) {
            System.out.println("[Erro] Nome da loja ou URL não podem estar vazios. Tente novamente.");
            continue;
        }
        
        Link link = new Link(loja, url, produto);
        produto.addLink(link);
        linkCount++;
    }

    service.add(produto);
    System.out.println("\nProduto e links cadastrados com sucesso!");
}

public void listarProdutos(ProdutoService service) {
    service.list();
}

public void editarProdutos(ProdutoService service) {
    System.out.println("Atualmente temos os seguintes produtos cadastrados: ");
    service.list();
    int indice = Integer.parseInt(IO.readln("Digite o indice do produto que deseja editar: "));

    Produto produto = (Produto) service.findByIndex(indice);
    
    System.out.println("\nOpções de Edição:");
    System.out.println("1 = Editar informações básicas do produto");
    System.out.println("2 = Gerenciar links (adicionar/limpar)");
    int opcaoEdicao = Integer.parseInt(IO.readln("Digite a opção desejada: "));
    
    if (opcaoEdicao == 1) {
        produto.setSku(IO.readln("Informe o novo SKU do produto: "));
        produto.setNome(IO.readln("Informe o novo nome do produto: "));
        produto.setDescricao(IO.readln("Informe a nova descricao do produto: "));
        produto.setMarca(IO.readln("Informe a nova marca do produto: "));
        produto.setPreco(Float.parseFloat(IO.readln("Informe o novo preco do produto: ")));
        service.edit(produto, produto.getId());
        System.out.println("Informações básicas editadas com sucesso!");
    } else if (opcaoEdicao == 2) {
        System.out.println("\nGerenciamento de Links do Produto " + produto.getNome() + ":");
        System.out.println("1 = Adicionar mais links");
        System.out.println("2 = Limpar todos os links e cadastrar novos");
        int subOpcao = Integer.parseInt(IO.readln("Digite a opção desejada: "));
        
        if (subOpcao == 2) {
            produto.getLinks().clear();
        }
        
        int linkCount = produto.getLinks().size();
        boolean cadastrando = true;
        while (cadastrando || linkCount < 2) {
            if (linkCount < 2) {
                System.out.printf("\n[Mínimo de 2 links exigido] Cadastro do Link #%d:\n", linkCount + 1);
            } else {
                String resposta = IO.readln("\nDeseja cadastrar mais um link de loja? (S/N): ");
                if (resposta.equalsIgnoreCase("N")) {
                    cadastrando = false;
                    break;
                }
            }
            
            String loja = IO.readln("Nome da loja (ex: Amazon): ");
            String url = IO.readln("URL do produto na loja: ");
            
            if (loja.trim().isEmpty() || url.trim().isEmpty()) {
                System.out.println("[Erro] Nome da loja ou URL não podem estar vazios. Tente novamente.");
                continue;
            }
            
            Link link = new Link(loja, url, produto);
            produto.addLink(link);
            linkCount++;
        }
        
        service.edit(produto, produto.getId());
        System.out.println("Links atualizados com sucesso!");
    } else {
        System.out.println("Opção inválida!");
    }
}

public void deletarProdutos(ProdutoService service) {
    System.out.println("Atualmente temos os seguintes produtos cadastrados: ");
    service.list();
    int indice = Integer.parseInt(IO.readln("Digite o indice do produto que deseja deletar: "));
    Produto produto = (Produto) service.findByIndex(indice);
    service.remove(produto);
}

public void executarCrawler(ProdutoService service) {
    CrawlerService crawler = new CrawlerService(service);
    crawler.executar();
}

public void visualizarHistoricoCompleto(ProdutoService service) {
    System.out.println("Atualmente temos os seguintes produtos cadastrados: ");
    service.list();
    int indice = Integer.parseInt(IO.readln("Digite o indice do produto para ver o histórico de preços completo: "));
    Produto produto = (Produto) service.findByIndex(indice);
    service.exibirHistoricoCompleto(produto);
}

public Integer menu() {
    System.out.println("Digite a opção desejada: ");
    System.out.println("1 = Adicionar um novo produto");
    System.out.println("2 = Listar os produtos");
    System.out.println("3 = Editar um produto");
    System.out.println("4 = Deletar um produto");
    System.out.println("5 = Executar o Crawler de preços");
    System.out.println("6 = Visualizar histórico de preços completo");
    System.out.println("0 = Sair");

    int opcao = Integer.parseInt(IO.readln());
    return opcao;
}
