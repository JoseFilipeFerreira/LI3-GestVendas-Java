package Model;

import Exceptions.*;
import Utils.Crono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GestVendasModel implements Serializable, IGestVendasModel{
    private static final long serialVersionUID = 6630744916568124368L;
    private int vendasLidas;
    private ICatCli catCli;
    private ICatProds catProds;
    private List<IVenda> vendas;
    private IFaturacao faturacao;
    private IFilial[] filiais;
    private IConstantes constantes;
    private final Crono time;

    /**
     * Construtuor do Model com toda a informacao necessaria para responder a qualquer pedido
     * @param configs Ficheiro com as configurações
     * @throws IOException Exceção se ocorrer erros a ler os ficheiros
     */
    public GestVendasModel(String configs) throws IOException{
        this.constantes = new Constantes(configs);
        this.time = new Crono();
        this.time.start();
        this.catCli = new CatCli(this.constantes.getClients());
        this.catProds = new CatProds(this.constantes.getProds());
        this.vendasLidas = 0;
        List<String> venda = new ArrayList<>();
        BufferedReader inFile = new BufferedReader(new FileReader(this.constantes.getSales()));
        String linha;
        while((linha = inFile.readLine()) != null)
                venda.add(linha);
        this.vendasLidas = venda.size();
        this.vendas = venda
                .parallelStream()
                .map(Venda::new)
                .filter(e -> e.validSale(this.constantes)
                        && catProds.exists(e.getCodProd())
                        && catCli.exists(e.getCodCli()))
                .collect(Collectors
                        .toList());
        faturacao = new Faturacao(catProds);
        this.filiais = new Filial[constantes.numeroFiliais()];
        for (int i = 0; i < constantes.numeroFiliais(); i++) {
            this.filiais[i] = new Filial();
        }
        this.vendas.forEach(e -> {this.faturacao.update(e); this.filiais[e.getFilial()-1].update(e);});
        this.time.stop();
    }
    /**
     * Construtuor do Model com toda a informacao necessaria para responder a qualquer pedido
     * @param clients Caminho do ficheiro de clientes
     * @param products Caminho do ficheiro de produtos
     * @param sales Caminho do ficheiro de vendas
     * @throws IOException Exceção se ocorrer erros a ler os ficheiros
     */
    public GestVendasModel(String clients, String products, String sales) throws IOException{
        this.constantes = new Constantes("db/configs.txt");
        this.time = new Crono();
        this.time.start();
        this.catCli = new CatCli(clients);
        this.catProds = new CatProds(products);
        this.vendasLidas = 0;
        List<String> venda = Files
                .readAllLines(Paths.get(sales), StandardCharsets.UTF_8);
        this.vendasLidas = venda.size();
        this.vendas = venda
                .parallelStream()
                .map(Venda::new)
                .filter(e -> e.validSale(this.constantes)
                        && catProds.exists(e.getCodProd())
                        && catCli.exists(e.getCodCli()))
                .collect(Collectors
                        .toList());
        faturacao = new Faturacao(catProds);
        this.filiais = new Filial[constantes.numeroFiliais()];
        for (int i = 0; i < constantes.numeroFiliais(); i++) {
            this.filiais[i] = new Filial();
        }
        this.vendas.forEach(e -> {this.faturacao.update(e); this.filiais[e.getFilial()-1].update(e);});
        this.time.stop();
    }

    /**
     * @return Tempo que demorou a ser inicializado o model
     */
    public String time() {
        return this.time.toString();
    }

    /**
     * @return Número de Filiais atual
     */
    public int numeroFiliais() {
        return this.constantes.numeroFiliais();
    }

    /**
     * @return Número de Meses
     */
    public int meses() {
        return this.constantes.meses();
    }

    /**
     * Calcula o numero de vendas a 0.0
     * @return Numero de vendas a 0
     */
    public int getVendasDadas() {
        return (int) this
                .vendas
                .stream()
                .filter(e -> e.totalSale() == 0)
                .count();
    }

    /**
     * Calcula o numero de vendas do ficheiro que sao invalidas
     * @return Numero de vendas invalidas
     */
    public int vendasInvalidas() {
        return this.vendasLidas - this.vendas.size();
    }

    /**
     * Getter do caminho do ficheiro de vendas
     * @return Caminho do ficheiro de vendas
     */
    public String getVendasFile() {
        return this.constantes.getSales();
    }

    /**
     * Getter do caminho do ficheiro de Produtos
     * @return Caminho do ficheiro de Produtos
     */
    public String getProductFile() {
        return this.constantes.getProds();
    }

    /**
     * Getter do caminho do ficheiro de Clientes
     * @return Caminho do ficheiro de Clientes
     */
    public String getClientFile() {
        return this.constantes.getClients();
    }

    /**
     * Calcula o numero total de produtos lidos
     * @return Total de produtos lidos
     */
    public int getTotalProdutos() {
        return this.catProds.howMany();
    }

    /**
     * Calcula o numero de produtos comprados e nao comprados
     * @return Numero de produtos comprados e nao comprados
     */
    public Map.Entry<Integer, Integer> getProdutosComprados() {
        int a = (int) this.vendas.stream().map(IVenda::getCodProd).distinct().count();
        return new AbstractMap.SimpleEntry<>(a, this.catProds.howMany() - a);
    }

    /**
     * Calcula o numero de clientes que fizeram e nao fizeram compras
     * @return Numero de clientes que fizeram e nao fizeram compras
     */
    public Map.Entry<Integer, Integer> getClientesCompradores() {
        int a = (int) this.vendas.stream()
                .map(IVenda::getCodCli)
                .distinct()
                .count();
        return new AbstractMap.SimpleEntry<>(a, this.catCli.howMany() - a);
    }

    /**
     * Calcula o total faturado em todas as vendas efetuadas
     * @return Total faturado
     */
    public double totalFaturado() {
        return this.faturacao.faturacaoTotal();
    }

    //1.2

    /**
     * Calcula o numero de vendas efetuadas por mes
     * @return Total de vendas feitas por mes
     */
    public Map<Integer, Integer> vendasMensais() {
        return this.vendas.stream()
                .map(IVenda::getMonth)
                .collect(Collectors.toMap(Function.identity(), e -> 1, Integer::sum));
    }

    /**
     * Calcula o total faturado por mes
     * @return Faturacao total por mes
     */
    public Map<Integer, Double> faturacaoTotal() {
        return this.faturacao.totalFaturado();
    }

    /**
     * Calcula o total faturado por mes numa filial
     * @param filial Filial da qual pretendemos a faturacao
     * @return Faturacao por mes da filial
     * @throws InvalidFilialException Exceção se a filial for inválida
     */
    public Map<Integer, Double> faturacaoPorFilial(int filial) throws InvalidFilialException {
        if(!constantes.filialValida(filial))
            throw new InvalidFilialException();
        return this.faturacao.totalFaturadoFilial(filial);
    }

    /**
     * Calcula o numero de clientes que fizeram compras numa filial por mes
     * @param filial Filial da qual se deseja a informacao
     * @return Numero de clientes que compraram na filial mes a mes
     * @throws InvalidFilialException Exceção se a filial for inválida
     */
    public Map<Integer, Integer> clientesPorFilial(int filial) throws InvalidFilialException {
        if(!this.constantes.filialValida(filial))
            throw new InvalidFilialException();
        return this.vendas.stream()
                .filter(e -> e.getFilial() == filial)
                .collect(Collectors.groupingBy(IVenda::getMonth))
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), (int) e.getValue().stream()
                        .distinct()
                        .count()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    //query 1
    /**
     * Calcula a lista ordenada dos Produtos que ninguem comprou
     * @return Lista de Produtos nao comprados
     */
    public List<String> listaDeProdutosNaoComprados() {
        return this.faturacao.listaProdutosNaoComprados();
    }

    //query 2 vendas, clientes

    /**
     * Calcula o total de vendas efetuadas e o numero de clientes que fizeram compras num determinado
     * mes
     * @param mes Mes referente as vendas
     * @return Total de vendas efetuadas e numero de clientes distintos que fizeram compras
     * @throws MesInvalidoException Exceção se o mês for inválido
     */
    public Map.Entry<Integer, Integer> clientesVendasTotais(int mes) throws MesInvalidoException {
        if(!constantes.mesValido(mes))
            throw new MesInvalidoException();
        List<IVenda> a = this.vendas.stream().filter(e -> e.getMonth() == mes).collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(a.size(), (int) a
                .stream()
                .map(IVenda::getCodCli)
                .distinct()
                .count());
    }

    /**
     * Calcula o total de vendas efetuadas e o numero de clientes que fizeram compras num
     * determindado mes e numa certa filial
     * @param filial Filial onde foram feitas as compras
     * @param mes Mes referente as vendas
     * @return Total de vendas efetuadas e numero de clientes distintos que fizeram compras
     * @throws InvalidFilialException Exceção se a filial for inválida
     * @throws MesInvalidoException Exceção se o mês for inválido
     */
    public Map.Entry<Integer, Integer> clientesVendasTotais(int filial, int mes) throws InvalidFilialException, MesInvalidoException {
        if(!constantes.filialValida(filial))
            throw new InvalidFilialException();
        if(!constantes.mesValido(mes))
            throw new MesInvalidoException();
        return this.filiais[filial-1].clientesVendasTotais(mes);
    }

    //query 3 (produtos comprados, n compras, quanto gastou)
    /**
     * Calcula o numero de produto diferentes comprados, quantas compras fez e quando gastou no total
     * de um dado cliente
     * @param clientID ID do cliente a procurar
     * @param mes Mes onde foram efetuadas as compras
     * @return Numero total de produtos distintos, quantas compras e quanto foi gasto pelo cliente num mes
     * @throws MesInvalidoException Exceção se o mês for inválido
     * @throws InvalidClientException Exceção se o cliente for inválido
     */
    public Map.Entry<Integer, Map.Entry<Integer,Double>> statsClientes(String clientID, int mes) throws MesInvalidoException, InvalidClientException {
        if(!constantes.mesValido(mes))
            throw new MesInvalidoException();
        if(!this.catCli.exists(clientID))
            throw new InvalidClientException();
        Set<String> ree = new HashSet<>();
        int vezes = 0;
        double total = 0;
        for(IFilial a : this.filiais) {
            Map.Entry<Set<String>, Map.Entry<Integer,Double>> o = a.statsCliente(clientID, mes);
            if(o != null) {
                ree.addAll(o.getKey());
                vezes += o.getValue().getKey();
                total += o.getValue().getValue();
            }
        }
        return new AbstractMap.SimpleEntry<>(ree.size(), new AbstractMap.SimpleEntry<>(vezes, total));
    }

     /**
     * Calcula o numero de clientes diferentes que compraram um determinado produto, quantas vezes foi comprado e quanto
      * foi faturado num mes
     * de um dado cliente
     * @param productID ID do produto a procurar
     * @param mes Mes onde foram efetuadas as compras
     * @return Numero total de clientes distintos que compraram o produto, quantas vezes foi comprado e quanto foi gasto
     * @throws MesInvalidoException Exceção se o mês for invalido
     * @throws InvalidProductExecption Exceção se o produto for inválido
     */
    public Map.Entry<Integer, Map.Entry<Integer,Double>> statsProdutos(String productID, int mes) throws MesInvalidoException, InvalidProductExecption {
        if(!constantes.mesValido(mes))
            throw new MesInvalidoException();
        if(!this.catProds.exists(productID))
            throw new InvalidProductExecption();
        Set<String> ree = new HashSet<>();
        int vezes = 0;
        double total = 0;
        for(IFilial a : this.filiais) {
            Map.Entry<Set<String>, Map.Entry<Integer,Double>> o = a.statsProduto(productID, mes);
            if(o != null) {
                ree.addAll(o.getKey());
                vezes += o.getValue().getKey();
                total += o.getValue().getValue();
            }
        }
        return new AbstractMap.SimpleEntry<>(ree.size(), new AbstractMap.SimpleEntry<>(vezes, total));
    }

    //Query 5
    /**
     * Determina a lista ordenada de produtos e quantas vezes um dado cliente
     * comprou
     * @param clientID Cliente do qual pretendemos a lista
     * @return Lista de produtos comprados por um cliente
     * @throws InvalidClientException Exceção se o Cliente não existir
     */
    public List<Map.Entry<String, Integer>> produtosPorCliente(String clientID) throws InvalidClientException {
        if(!this.catCli.exists(clientID))
            throw new InvalidClientException();
        List<Map<String, Integer>> b = new ArrayList<>();
        for(IFilial a : this.filiais) {
            b.add(a.produtosCompradosPorCliente(clientID));
        }
        return b.stream()
                .filter(Objects::nonNull)
                .flatMap(e -> e.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum))
                .entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    int a = Integer.compare(o2.getValue(), o1.getValue());
                    if(a == 0)
                        a = o1.getKey().compareTo(o2.getKey());
                    return a;
                })
                .collect(Collectors.toList());
    }

    //query 6

    /**
     * Determina a lista ordenada dos produtos mais vendidos durante o ano,
     * bem como quem os comprou
     * @return Lista ordenada dos produtos mais vendidos
     */
    public List<Map.Entry<String, Integer>> produtosMaisVendidos() {
        return this.vendas.stream()
                .collect(Collectors.toMap(IVenda::getCodProd,
                        e -> Stream.of(e)
                                .collect(Collectors.toList()),
                        (e1, e2) -> {e1.addAll(e2);
                            return e1;}))
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                        e.getValue()
                                .stream()
                                .collect(Collectors.toMap(IVenda::getCodCli,
                                        IVenda::getQuant,
                                        Integer::sum))))
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                        new AbstractMap.SimpleEntry<>(e.getValue().size(),
                                e.getValue().values().stream()
                                        .mapToInt(Integer::intValue)
                                        .sum())))
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue(Map.Entry.comparingByValue(Integer::compareTo))))
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getKey()))
                .collect(Collectors.toList());
    }

    //query 7

    /**
     * Determina o top de clientes da filial
     * @param filial Filial em questao
     * @return Lista com IDs do top de clientes
     * @throws InvalidFilialException Exceção se a filial for inválida
     */
    public List<String> melhoresClientesPorFilial(int filial) throws InvalidFilialException {
        if(!this.constantes.filialValida(filial))
            throw new InvalidFilialException();
        return this.filiais[filial-1].getBestBuyers();
    }

    //query 8

    /**
     * Determina a lista de clientes que compraram mais produtos diferentes
     * @return Lista de clientes ordenada pelo numero de produtos distintos
     * comprados
     */
    public List<String> clientesComMaisDiversidade() {
        List<Map<String, Set<String>>> a = new ArrayList<>();
        for(IFilial x : this.filiais) {
            Map<String, Set<String>> o = x.maisDiversidadeDeProdutos();
            if(o != null)
                a.add(o);
        }
        return a.stream()
                .flatMap(e -> e.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> {e1.addAll(e2); return e1;}))
                .entrySet()
                .stream()
                .sorted(Collections
                        .reverseOrder(Map.Entry
                                .comparingByValue(Comparator.comparingInt(Set::size))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    //query 9

    /**
     * Determina a lista de clientes que mais compraram um dado produto e quanto
     * gastaram no total
     * @param prodID Codigo do Produto em questao
     * @return Lista ordenada dos clientes que mais compraram o produto
     * @throws InvalidProductExecption Exceção se o Produto não existir
     */
    public List<Map.Entry<String,Double>> clientesQueMaisCompraram(String prodID) throws InvalidProductExecption {
        if(!this.catProds.exists(prodID))
            throw new InvalidProductExecption();
        return Arrays.stream(filiais)
                .map(e -> e.clientesQueMaisCompraram(prodID))
                .filter(Objects::nonNull)
                .flatMap(e -> e.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Double::sum))
                .entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    int a = Double.compare(o2.getValue(), o1.getValue());
                    if(a == 0)
                        a = o2.getKey().compareTo(o1.getKey());
                    return a;
                })
                .collect(Collectors.toList());
    }

    //query 10

    /**
     * Determina a faturacao por produto total num mes numa filial
     * @param mes Mes da faturacao
     * @param filial Filial referente
     * @return Lista de produtos comprados na filial e respetiva faturacao
     * @throws InvalidFilialException Exceção se a filial não existir
     * @throws MesInvalidoException Exceção se o mês for inválido
     */
    public Map<String, Double> faturacaoProd(int mes, int filial) throws InvalidFilialException, MesInvalidoException {
        if(!constantes.filialValida(filial))
            throw new InvalidFilialException();
        if(!constantes.mesValido(mes))
            throw new MesInvalidoException();
        return this.vendas.stream()
                .filter(e -> e.getFilial() == filial && e.getMonth() == mes)
                .collect(Collectors.toMap(IVenda::getCodProd, IVenda::totalSale, Double::sum));
    }

    /**
     * Guarda o estado atual do Model para um dado ficheiro
     * @param fName Caminho do ficheiro a guardar
     * @throws IOException Exceção de erro a escrever para o ficheiro
     */
    public void save(String fName) throws IOException {
        FileOutputStream a = new FileOutputStream(fName);
        ObjectOutputStream r = new ObjectOutputStream(a);
        r.writeObject(this);
        r.flush();
        r.close();
    }

    /**
     * Carrega o Model de um ficheiro de ObjectStream
     * @param fName Caminho do ficheiro a carregar
     * @return Modelo lido
     * @throws IOException Erro a ler do ficheiro
     * @throws ClassNotFoundException O ficheiro lido é invalido
     */
    public static GestVendasModel read(String fName) throws IOException, ClassNotFoundException {
        FileInputStream r = new FileInputStream(fName);
        ObjectInputStream a = new ObjectInputStream(r);
        GestVendasModel u = (GestVendasModel) a.readObject();
        a.close();
        return u;
    }
}
